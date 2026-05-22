"""
Tier 2 — Config-based scraper engine.
Reads CSS selectors from DB config. Works for any website with stable HTML.
Add new websites via POST /api/v1/scrape/configs — no redeploy needed.
"""
import logging
import re
from bs4 import BeautifulSoup
from shared.models import ScrapedProductData, ScrapedSellerListing, ScraperConfig
from shared.utils import clean_price
from app.core.base_scraper import BaseScraper

logger = logging.getLogger(__name__)


class ConfigBasedScraper(BaseScraper):
    def __init__(self, config: ScraperConfig):
        self.config = config

    async def scrape_product(self, url: str) -> tuple[ScrapedProductData, list[ScrapedSellerListing]]:
        if self.config.type == "dynamic":
            html = await self.playwright_render(url)
        else:
            html = await self.http_get(url)

        soup = BeautifulSoup(html, "lxml")
        selectors = self.config.selectors

        name = self._extract_text(soup, selectors.get("product_name", ""))
        price_str = self._extract_text(soup, selectors.get("price", ""))
        original_price_str = self._extract_text(soup, selectors.get("original_price", ""))
        image_urls = self._extract_all_attr(soup, selectors.get("images", ""), "src")
        specs = self._extract_specs(soup, selectors.get("specs", ""))

        product_data = ScrapedProductData(
            name=name,
            image_urls=image_urls[:8],
            specs=specs,
        )

        seller_listing = ScrapedSellerListing(
            seller_name=self.config.name,
            external_url=url,
            current_price=clean_price(price_str),
            original_price=clean_price(original_price_str),
        )

        logger.info(f"[Tier2:{self.config.domain}] Scraped: {name}")
        return product_data, [seller_listing]

    def _extract_text(self, soup: BeautifulSoup, selector: str) -> str:
        if not selector:
            return ""
        el = soup.select_one(selector)
        return el.get_text(strip=True) if el else ""

    def _extract_all_attr(self, soup: BeautifulSoup, selector: str, attr: str) -> list[str]:
        if not selector:
            return []
        return [el.get(attr, "") for el in soup.select(selector) if el.get(attr)]

    def _extract_specs(self, soup: BeautifulSoup, selector: str) -> dict:
        if not selector:
            return {}
        specs = {}
        for el in soup.select(selector)[:30]:
            text = el.get_text(" ", strip=True)
            if ":" in text:
                k, v = text.split(":", 1)
                specs[k.strip()] = v.strip()
        return specs
