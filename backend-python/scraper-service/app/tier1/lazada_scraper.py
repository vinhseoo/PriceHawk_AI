"""
Tier 1 — Lazada scraper.
Lazada does not have a stable public API. Strategy:
  1. Playwright renders the page (JS-heavy SPA)
  2. Parse window.__INITIAL_STATE__ JSON embedded in the HTML
  3. Fall back to meta-tag + BeautifulSoup extraction if state is absent

URL format: https://www.lazada.vn/products/{name}-i{itemId}-s{skuId}.html
"""
import json
import logging
import re

import httpx
from bs4 import BeautifulSoup
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from shared.models import ScrapedProductData, ScrapedSellerListing, ScrapedReview
from app.core.base_scraper import BaseScraper

logger = logging.getLogger(__name__)

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept-Language": "vi-VN,vi;q=0.9,en;q=0.8",
}


def _parse_item_sku_id(url: str) -> tuple[str, str]:
    """Extract itemId and skuId from Lazada URL.
    https://www.lazada.vn/products/abc-i3210697819-s16264012524.html
    """
    item_match = re.search(r"-i(\d+)-", url)
    sku_match = re.search(r"-s(\d+)\.", url)
    item_id = item_match.group(1) if item_match else ""
    sku_id = sku_match.group(1) if sku_match else ""
    return item_id, sku_id


class LazadaScraper(BaseScraper):

    @retry(
        retry=retry_if_exception_type(Exception),
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        reraise=True,
    )
    async def scrape_product(self, url: str) -> tuple[ScrapedProductData, list[ScrapedSellerListing]]:
        item_id, sku_id = _parse_item_sku_id(url)

        # Playwright renders the full SPA; embedded JSON has all product details
        html = await self.playwright_render(url)

        product_data, seller_listing = self._parse_initial_state(html, url, item_id, sku_id)

        logger.info(f"[Lazada] Scraped: {product_data.name} — "
                    f"{seller_listing.current_price:,.0f} VND" if seller_listing.current_price else
                    f"[Lazada] Scraped: {product_data.name}")
        return product_data, [seller_listing]

    def _parse_initial_state(
        self,
        html: str,
        url: str,
        item_id: str,
        sku_id: str,
    ) -> tuple[ScrapedProductData, ScrapedSellerListing]:
        """
        Parse window.__INITIAL_STATE__ JSON injected into Lazada product pages.
        Falls back to BeautifulSoup meta extraction if the state is not found.
        """
        # Try window.__INITIAL_STATE__ or window.pageData
        state_data = self._extract_json_state(html)

        if state_data:
            return self._parse_from_state(state_data, url, item_id, sku_id)

        # Fallback: BeautifulSoup meta extraction
        logger.warning(f"[Lazada] No __INITIAL_STATE__ found, falling back to meta extraction for {url}")
        return self._parse_from_meta(html, url, item_id, sku_id)

    def _extract_json_state(self, html: str) -> dict | None:
        """Extract JSON from window.__INITIAL_STATE__ = {...}; or window.pageData = {...};"""
        patterns = [
            r"window\.__INITIAL_STATE__\s*=\s*(\{.+?\});?\s*(?:window|</script>)",
            r"window\.pageData\s*=\s*(\{.+?\});?\s*(?:window|</script>)",
        ]
        for pattern in patterns:
            match = re.search(pattern, html, re.DOTALL)
            if match:
                try:
                    return json.loads(match.group(1))
                except json.JSONDecodeError:
                    continue
        return None

    def _parse_from_state(
        self,
        state: dict,
        url: str,
        item_id: str,
        sku_id: str,
    ) -> tuple[ScrapedProductData, ScrapedSellerListing]:
        # Navigate common state structures
        page_info = (
            state.get("pdp", {})
            or state.get("product", {})
            or state.get("pageData", {})
        )
        product = page_info.get("product") or page_info

        name = (
            product.get("name")
            or product.get("title")
            or product.get("productName", "Unknown Product")
        )
        brand = product.get("brand", {})
        brand_name = brand.get("name") if isinstance(brand, dict) else str(brand) if brand else None

        # Price — Lazada uses "price" as string with currency symbol sometimes
        price_info = product.get("price") or {}
        if isinstance(price_info, dict):
            price = self._parse_price(str(price_info.get("priceShow") or price_info.get("price") or 0))
            original_price = self._parse_price(str(price_info.get("originalPrice") or 0))
        else:
            price = self._parse_price(str(price_info))
            original_price = None

        images = []
        for img in product.get("images") or []:
            if isinstance(img, str):
                images.append(img)
            elif isinstance(img, dict):
                images.append(img.get("url") or img.get("src") or "")
        images = [i for i in images if i][:8]

        # Specs from attributes
        specs: dict[str, str] = {}
        for attr in (product.get("attributes") or product.get("props") or []):
            if isinstance(attr, dict):
                k = attr.get("name") or attr.get("label") or ""
                v = attr.get("value") or attr.get("val") or ""
                if k and v:
                    specs[str(k)] = str(v)

        seller_info = product.get("seller") or state.get("seller") or {}
        seller_name = (
            seller_info.get("name") or seller_info.get("sellerName") or "Lazada"
        ) if isinstance(seller_info, dict) else "Lazada"

        product_data = ScrapedProductData(
            name=name,
            brand=brand_name,
            image_urls=images,
            specs=specs,
        )
        seller_listing = ScrapedSellerListing(
            seller_name=seller_name,
            external_url=url,
            external_product_id=item_id or sku_id or None,
            current_price=price,
            original_price=original_price if original_price and original_price != price else None,
        )
        return product_data, seller_listing

    def _parse_from_meta(
        self,
        html: str,
        url: str,
        item_id: str,
        sku_id: str,
    ) -> tuple[ScrapedProductData, ScrapedSellerListing]:
        """Last-resort extraction from Open Graph meta tags."""
        soup = BeautifulSoup(html, "lxml")

        name = (
            (soup.find("meta", property="og:title") or {}).get("content")
            or (soup.find("h1") or {}).get_text(strip=True)
            or "Unknown Product"
        )
        image = (soup.find("meta", property="og:image") or {}).get("content")
        price_meta = (soup.find("meta", property="product:price:amount") or {}).get("content")

        product_data = ScrapedProductData(
            name=name,
            image_urls=[image] if image else [],
        )
        seller_listing = ScrapedSellerListing(
            seller_name="Lazada",
            external_url=url,
            external_product_id=item_id or None,
            current_price=float(price_meta) if price_meta else None,
        )
        return product_data, seller_listing

    @staticmethod
    def _parse_price(price_str: str) -> float | None:
        """Remove currency symbols and separators, return float."""
        cleaned = re.sub(r"[^\d.]", "", price_str)
        # Lazada VN prices are integers — remove any trailing .0
        try:
            return float(cleaned) if cleaned else None
        except ValueError:
            return None
