"""Tier 1 — Lazada API-based scraper. TODO: implement in Phase 4 Task 4.6."""
import logging
from shared.models import ScrapedProductData, ScrapedSellerListing
from app.core.base_scraper import BaseScraper

logger = logging.getLogger(__name__)


class LazadaScraper(BaseScraper):
    async def scrape_product(self, url: str) -> tuple[ScrapedProductData, list[ScrapedSellerListing]]:
        # TODO Phase 4 Task 4.6: implement Lazada API scraping
        raise NotImplementedError("LazadaScraper not yet implemented. See Phase 4 Task 4.6.")
