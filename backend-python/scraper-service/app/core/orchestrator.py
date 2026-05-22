import logging
from shared.models import ScrapeResultEvent, ScraperTier, SourceType
from shared.utils import extract_domain

logger = logging.getLogger(__name__)

TIER1_DOMAINS = {
    "shopee.vn": "shopee",
    "lazada.vn": "lazada",
    "tiki.vn": "tiki",
}


class ScraperOrchestrator:
    """Routes URLs to the correct scraper tier."""

    def __init__(self, config_service, llm_client=None):
        self._config_service = config_service
        self._llm_client = llm_client

    async def scrape(self, url: str, job_id: str, discover_sellers: bool = False) -> ScrapeResultEvent:
        domain = extract_domain(url)
        logger.info(f"Scraping URL: {url} (domain={domain})")

        # Tier 1: API-based scrapers
        if domain in TIER1_DOMAINS:
            return await self._scrape_tier1(url, domain, job_id, discover_sellers)

        # Tier 2: Config-based scrapers
        config = await self._config_service.get_active_config(domain)
        if config:
            return await self._scrape_tier2(url, domain, config, job_id)

        # Tier 3: AI Generic scraper
        return await self._scrape_tier3(url, domain, job_id)

    async def _scrape_tier1(self, url: str, domain: str, job_id: str, discover_sellers: bool) -> ScrapeResultEvent:
        from app.tier1.shopee_scraper import ShopeeScraper
        from app.tier1.lazada_scraper import LazadaScraper
        from app.tier1.tiki_scraper import TikiScraper

        scrapers = {"shopee": ShopeeScraper, "lazada": LazadaScraper, "tiki": TikiScraper}
        scraper_cls = scrapers[TIER1_DOMAINS[domain]]
        scraper = scraper_cls()
        product_data, seller_listings = await scraper.scrape_product(url)

        return ScrapeResultEvent(
            job_id=job_id,
            domain=domain,
            platform=TIER1_DOMAINS[domain].upper(),
            source_type=SourceType.MARKETPLACE,
            scraper_tier=ScraperTier.API_BASED,
            product_data=product_data,
            seller_listings=seller_listings,
        )

    async def _scrape_tier2(self, url: str, domain: str, config, job_id: str) -> ScrapeResultEvent:
        from app.tier2.config_scraper import ConfigBasedScraper
        scraper = ConfigBasedScraper(config)
        product_data, seller_listings = await scraper.scrape_product(url)

        return ScrapeResultEvent(
            job_id=job_id,
            domain=domain,
            platform="OTHER",
            source_type=SourceType.RETAILER,
            scraper_tier=ScraperTier.CONFIG_BASED,
            product_data=product_data,
            seller_listings=seller_listings,
        )

    async def _scrape_tier3(self, url: str, domain: str, job_id: str) -> ScrapeResultEvent:
        from app.tier3.ai_scraper import AIGenericScraper
        scraper = AIGenericScraper(self._llm_client)
        product_data, seller_listings = await scraper.scrape_product(url)

        # Auto-generate config suggestion
        await scraper.auto_generate_config(url, product_data, self._config_service)

        return ScrapeResultEvent(
            job_id=job_id,
            domain=domain,
            platform="OTHER",
            source_type=SourceType.UNKNOWN,
            scraper_tier=ScraperTier.AI_GENERIC,
            product_data=product_data,
            seller_listings=seller_listings,
        )
