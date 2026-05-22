"""
Tier 1 — Shopee API-based scraper.
Calls Shopee internal API endpoints directly → structured JSON → no HTML parsing needed.
"""
import logging
import httpx
from shared.models import ScrapedProductData, ScrapedSellerListing, ScrapedReview
from app.core.base_scraper import BaseScraper

logger = logging.getLogger(__name__)

SHOPEE_API = "https://shopee.vn/api/v4"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Referer": "https://shopee.vn",
    "X-API-SOURCE": "pc",
}


def _parse_shop_item_id(url: str) -> tuple[str, str]:
    """Extract shopid and itemid from Shopee URL."""
    import re
    match = re.search(r"-i\.(\d+)\.(\d+)", url)
    if match:
        return match.group(1), match.group(2)
    raise ValueError(f"Cannot parse Shopee URL: {url}")


class ShopeeScraper(BaseScraper):
    async def scrape_product(self, url: str) -> tuple[ScrapedProductData, list[ScrapedSellerListing]]:
        shop_id, item_id = _parse_shop_item_id(url)

        async with httpx.AsyncClient(headers=HEADERS, timeout=20) as client:
            resp = await client.get(
                f"{SHOPEE_API}/item/get",
                params={"shopid": shop_id, "itemid": item_id},
            )
            resp.raise_for_status()
            data = resp.json().get("data", {})

        if not data:
            raise ValueError(f"No data from Shopee API for {url}")

        product_data = ScrapedProductData(
            name=data.get("name", ""),
            brand=data.get("brand", None),
            description=data.get("description", ""),
            thumbnail_url=f"https://cf.shopee.vn/file/{data.get('image', '')}",
            image_urls=[f"https://cf.shopee.vn/file/{img}" for img in data.get("images", [])[:8]],
            specs={attr["name"]: attr["value"] for attr in data.get("attributes", [])},
        )

        price_min = data.get("price_min", 0) / 100000  # Shopee uses price * 100000
        price_max = data.get("price_max", 0) / 100000
        original_price = data.get("price_before_discount", 0)

        seller_listing = ScrapedSellerListing(
            seller_name=data.get("shop_name", f"Shop {shop_id}"),
            seller_id=str(shop_id),
            seller_url=f"https://shopee.vn/shop/{shop_id}",
            is_official_store=data.get("is_official_shop", False),
            external_url=url,
            external_product_id=str(item_id),
            current_price=price_min,
            original_price=original_price / 100000 if original_price else None,
            review_count=data.get("comment_count", 0),
            average_rating=data.get("item_rating", {}).get("rating_star", None),
        )

        logger.info(f"Shopee scraped: {product_data.name} — {price_min:,.0f} VND")
        return product_data, [seller_listing]
