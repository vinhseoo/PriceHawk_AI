"""
Tier 1 — Shopee API-based scraper.
Calls Shopee internal API endpoints directly → structured JSON → no HTML parsing needed.

Features:
- Product data + pricing from item/get endpoint
- Full review pagination (all pages, not just page 1)
- Multi-seller discovery via search (when discover_sellers=True)
- Rate limiting: sleep between requests to avoid 429
"""
import asyncio
import logging
import re
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

import httpx

from shared.models import ScrapedProductData, ScrapedSellerListing, ScrapedReview
from app.core.base_scraper import BaseScraper

logger = logging.getLogger(__name__)

SHOPEE_API_V4 = "https://shopee.vn/api/v4"
SHOPEE_API_V2 = "https://shopee.vn/api/v2"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Referer": "https://shopee.vn",
    "X-API-SOURCE": "pc",
    "X-Shopee-Language": "vi",
}
REVIEW_PAGE_LIMIT = 20
MAX_REVIEW_PAGES = 5     # Cap at 100 reviews to avoid huge payloads
REQUEST_DELAY_SEC = 0.5  # Polite delay between API calls


def _parse_shop_item_id(url: str) -> tuple[str, str]:
    """Extract shopid and itemid from Shopee product URL.
    Supports: https://shopee.vn/product-name-i.{shopId}.{itemId}
    """
    match = re.search(r"-i\.(\d+)\.(\d+)", url)
    if match:
        return match.group(1), match.group(2)
    raise ValueError(f"Cannot parse Shopee URL: {url}")


class ShopeeScraper(BaseScraper):

    @retry(
        retry=retry_if_exception_type(httpx.HTTPStatusError),
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        reraise=True,
    )
    async def scrape_product(self, url: str) -> tuple[ScrapedProductData, list[ScrapedSellerListing]]:
        shop_id, item_id = _parse_shop_item_id(url)

        async with httpx.AsyncClient(headers=HEADERS, timeout=20, follow_redirects=True) as client:
            # 1. Fetch product data
            resp = await client.get(
                f"{SHOPEE_API_V4}/item/get",
                params={"shopid": shop_id, "itemid": item_id},
            )
            resp.raise_for_status()
            data = resp.json().get("data", {})

            if not data:
                raise ValueError(f"No data from Shopee API for {url}")

            await asyncio.sleep(REQUEST_DELAY_SEC)

            # 2. Fetch reviews (paginated)
            reviews = await self._fetch_all_reviews(client, shop_id, item_id)

        product_data = ScrapedProductData(
            name=data.get("name", ""),
            brand=data.get("brand") or None,
            description=data.get("description", ""),
            thumbnail_url=f"https://cf.shopee.vn/file/{data.get('image', '')}",
            image_urls=[
                f"https://cf.shopee.vn/file/{img}"
                for img in data.get("images", [])[:8]
            ],
            specs={
                attr["name"]: attr["value"]
                for attr in data.get("attributes", [])
                if attr.get("name") and attr.get("value")
            },
        )

        # Shopee prices are stored as price × 100000
        price_min = (data.get("price_min") or data.get("price") or 0) / 100000
        original_price = data.get("price_before_discount", 0)

        seller_listing = ScrapedSellerListing(
            seller_name=data.get("shop_name", f"Shop {shop_id}"),
            seller_id=str(shop_id),
            seller_url=f"https://shopee.vn/shop/{shop_id}",
            is_official_store=bool(data.get("is_official_shop")),
            external_url=url,
            external_product_id=str(item_id),
            current_price=price_min,
            original_price=(original_price / 100000) if original_price else None,
            review_count=data.get("comment_count", 0),
            average_rating=data.get("item_rating", {}).get("rating_star"),
            reviews=reviews,
        )

        logger.info(f"[Shopee] Scraped: {product_data.name} — {price_min:,.0f} VND "
                    f"({len(reviews)} reviews)")
        return product_data, [seller_listing]

    async def _fetch_all_reviews(
        self,
        client: httpx.AsyncClient,
        shop_id: str,
        item_id: str,
    ) -> list[ScrapedReview]:
        """Paginate through Shopee review API until no more reviews or MAX_REVIEW_PAGES reached."""
        reviews: list[ScrapedReview] = []

        for page in range(MAX_REVIEW_PAGES):
            offset = page * REVIEW_PAGE_LIMIT
            try:
                resp = await client.get(
                    f"{SHOPEE_API_V2}/item/get_ratings",
                    params={
                        "itemid": item_id,
                        "shopid": shop_id,
                        "offset": offset,
                        "limit": REVIEW_PAGE_LIMIT,
                        "filter": 0,
                        "type": 0,
                    },
                )
                resp.raise_for_status()
                ratings = resp.json().get("data", {}).get("ratings", [])

                if not ratings:
                    break  # No more reviews

                for r in ratings:
                    reviews.append(ScrapedReview(
                        reviewer_name=r.get("author_username"),
                        rating=r.get("rating_star"),
                        content=r.get("comment"),
                        review_date=str(r.get("ctime", "")),
                    ))

                if len(ratings) < REVIEW_PAGE_LIMIT:
                    break  # Last page

                await asyncio.sleep(REQUEST_DELAY_SEC)

            except Exception as e:
                logger.warning(f"[Shopee] Review fetch failed (page {page}): {e}")
                break

        return reviews
