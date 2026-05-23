"""
Tier 1 — Tiki API-based scraper.
Uses Tiki's public product + review APIs (no auth required for read).

URL format: https://tiki.vn/{slug}-p{productId}.html
            https://tiki.vn/sp/{slug}-p{productId}.html
"""
import asyncio
import logging
import re

import httpx
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from shared.models import ScrapedProductData, ScrapedSellerListing, ScrapedReview
from app.core.base_scraper import BaseScraper

logger = logging.getLogger(__name__)

TIKI_API = "https://tiki.vn/api/v2"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Accept": "application/json, text/plain, */*",
    "Accept-Language": "vi-VN,vi;q=0.9",
    "Referer": "https://tiki.vn",
}
REVIEW_PAGE_SIZE = 20
MAX_REVIEW_PAGES = 5
REQUEST_DELAY_SEC = 0.5


def _parse_product_id(url: str) -> str:
    """Extract product ID from Tiki URL.
    Supports: /product-name-p123456.html and /sp/product-name-p123456.html
    """
    match = re.search(r"-p(\d+)\.html", url)
    if match:
        return match.group(1)
    raise ValueError(f"Cannot parse Tiki product ID from URL: {url}")


class TikiScraper(BaseScraper):

    @retry(
        retry=retry_if_exception_type(httpx.HTTPStatusError),
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        reraise=True,
    )
    async def scrape_product(self, url: str) -> tuple[ScrapedProductData, list[ScrapedSellerListing]]:
        product_id = _parse_product_id(url)

        async with httpx.AsyncClient(headers=HEADERS, timeout=20, follow_redirects=True) as client:
            # 1. Fetch product
            resp = await client.get(
                f"{TIKI_API}/products/{product_id}",
                params={"platform": "web"},
            )
            resp.raise_for_status()
            data = resp.json()

            await asyncio.sleep(REQUEST_DELAY_SEC)

            # 2. Fetch reviews
            reviews = await self._fetch_all_reviews(client, product_id)

        # Parse product data
        images = [img.get("base_url", "") for img in data.get("images", [])[:8] if img.get("base_url")]
        specs: dict[str, str] = {}
        for spec_group in data.get("specifications", []):
            for attr in spec_group.get("attributes", []):
                if attr.get("name") and attr.get("value"):
                    specs[attr["name"]] = attr["value"]

        product_data = ScrapedProductData(
            name=data.get("name", ""),
            brand=data.get("brand", {}).get("name") if data.get("brand") else None,
            description=data.get("description", ""),
            thumbnail_url=data.get("thumbnail_url"),
            image_urls=images,
            specs=specs,
        )

        # Current seller listing (the default seller for this product)
        price = data.get("price", 0)
        original_price = data.get("original_price")
        current_seller = data.get("current_seller", {})

        seller_listing = ScrapedSellerListing(
            seller_name=current_seller.get("name", "Tiki"),
            seller_id=str(current_seller.get("id", "")),
            seller_url=f"https://tiki.vn/cua-hang/{current_seller.get('slug', '')}",
            is_official_store=bool(data.get("is_authentic")),
            external_url=url,
            external_product_id=str(product_id),
            current_price=float(price),
            original_price=float(original_price) if original_price and original_price != price else None,
            review_count=data.get("review_count", 0),
            average_rating=data.get("rating_average"),
            reviews=reviews,
        )

        logger.info(f"[Tiki] Scraped: {product_data.name} — {price:,.0f} VND ({len(reviews)} reviews)")
        return product_data, [seller_listing]

    async def _fetch_all_reviews(
        self,
        client: httpx.AsyncClient,
        product_id: str,
    ) -> list[ScrapedReview]:
        """Paginate Tiki review API."""
        reviews: list[ScrapedReview] = []

        for page in range(1, MAX_REVIEW_PAGES + 1):
            try:
                resp = await client.get(
                    f"{TIKI_API}/reviews",
                    params={
                        "product_id": product_id,
                        "page": page,
                        "limit": REVIEW_PAGE_SIZE,
                        "sort": "score|desc,id|desc,stars|all",
                    },
                )
                resp.raise_for_status()
                payload = resp.json()
                items = payload.get("data", [])

                if not items:
                    break

                for r in items:
                    reviews.append(ScrapedReview(
                        reviewer_name=r.get("created_by", {}).get("name"),
                        rating=r.get("rating"),
                        content=r.get("content"),
                        review_date=r.get("created_at"),
                    ))

                paging = payload.get("paging", {})
                if page >= paging.get("last_page", 1):
                    break

                await asyncio.sleep(REQUEST_DELAY_SEC)

            except Exception as e:
                logger.warning(f"[Tiki] Review fetch failed (page {page}): {e}")
                break

        return reviews
