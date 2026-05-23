"""
Tests for ScraperOrchestrator — tier routing logic.
"""
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from shared.models import ScraperTier, SourceType, ScrapeResultEvent, ScrapedProductData, ScrapedSellerListing
from app.core.orchestrator import ScraperOrchestrator


def _make_scrape_result(tier: ScraperTier, domain: str = "shopee.vn") -> ScrapeResultEvent:
    return ScrapeResultEvent(
        job_id="job-abc",
        domain=domain,
        scraper_tier=tier,
        source_type=SourceType.MARKETPLACE,
        product_data=ScrapedProductData(name="Test Product"),
        seller_listings=[
            ScrapedSellerListing(
                seller_name="Test Seller",
                external_url=f"https://{domain}/product",
            )
        ],
    )


@pytest.fixture
def mock_config_service():
    svc = MagicMock()
    svc.get_active_config = AsyncMock(return_value=None)
    svc.save_ai_suggestion = AsyncMock()
    return svc


class TestOrchestratorTierRouting:
    @pytest.mark.asyncio
    async def test_shopee_url_routes_to_tier1(self, mock_config_service):
        orchestrator = ScraperOrchestrator(mock_config_service)

        tier1_result = _make_scrape_result(ScraperTier.API_BASED, "shopee.vn")

        with patch("app.tier1.shopee_scraper.ShopeeScraper") as MockScraper:
            instance = MockScraper.return_value
            instance.scrape_product = AsyncMock(
                return_value=(tier1_result.product_data, tier1_result.seller_listings)
            )

            result = await orchestrator.scrape(
                url="https://shopee.vn/product-i.111.222",
                job_id="job-1",
            )

        assert result.scraper_tier == ScraperTier.API_BASED
        mock_config_service.get_active_config.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_tiki_url_routes_to_tier1(self, mock_config_service):
        orchestrator = ScraperOrchestrator(mock_config_service)

        tier1_result = _make_scrape_result(ScraperTier.API_BASED, "tiki.vn")

        with patch("app.tier1.tiki_scraper.TikiScraper") as MockScraper:
            instance = MockScraper.return_value
            instance.scrape_product = AsyncMock(
                return_value=(tier1_result.product_data, tier1_result.seller_listings)
            )

            result = await orchestrator.scrape(
                url="https://tiki.vn/product-p123456.html",
                job_id="job-2",
            )

        assert result.scraper_tier == ScraperTier.API_BASED

    @pytest.mark.asyncio
    async def test_unknown_domain_with_config_routes_to_tier2(self, mock_config_service):
        from shared.models import ScraperConfig, ScraperConfigStatus
        config = ScraperConfig(
            id="cfg-1",
            domain="sendo.vn",
            name="Sendo",
            selectors={"name": "h1.product-title"},
            type="static",
        )
        mock_config_service.get_active_config = AsyncMock(return_value=config)

        orchestrator = ScraperOrchestrator(mock_config_service)
        tier2_product = ScrapedProductData(name="Sendo Product")
        tier2_listing = ScrapedSellerListing(
            seller_name="Sendo", external_url="https://sendo.vn/p/123"
        )

        with patch("app.tier2.config_scraper.ConfigBasedScraper") as MockScraper:
            instance = MockScraper.return_value
            instance.scrape_product = AsyncMock(return_value=(tier2_product, [tier2_listing]))

            result = await orchestrator.scrape(
                url="https://sendo.vn/product/123",
                job_id="job-3",
            )

        assert result.scraper_tier == ScraperTier.CONFIG_BASED
        mock_config_service.get_active_config.assert_awaited_once_with("sendo.vn")

    @pytest.mark.asyncio
    async def test_unknown_domain_no_config_routes_to_tier3(self, mock_config_service):
        mock_config_service.get_active_config = AsyncMock(return_value=None)
        mock_llm = MagicMock()
        orchestrator = ScraperOrchestrator(mock_config_service, mock_llm)

        tier3_product = ScrapedProductData(name="Unknown Product")
        tier3_listing = ScrapedSellerListing(
            seller_name="Unknown Store", external_url="https://unknown-store.vn/p/abc"
        )

        with patch("app.tier3.ai_scraper.AIGenericScraper") as MockScraper:
            instance = MockScraper.return_value
            instance.scrape_product = AsyncMock(return_value=(tier3_product, [tier3_listing]))
            instance.auto_generate_config = AsyncMock()

            result = await orchestrator.scrape(
                url="https://unknown-store.vn/p/abc",
                job_id="job-4",
            )

        assert result.scraper_tier == ScraperTier.AI_GENERIC

    @pytest.mark.asyncio
    async def test_result_contains_correct_job_id(self, mock_config_service):
        orchestrator = ScraperOrchestrator(mock_config_service)

        tier1_result = _make_scrape_result(ScraperTier.API_BASED)

        with patch("app.tier1.shopee_scraper.ShopeeScraper") as MockScraper:
            instance = MockScraper.return_value
            instance.scrape_product = AsyncMock(
                return_value=(tier1_result.product_data, tier1_result.seller_listings)
            )

            result = await orchestrator.scrape(
                url="https://shopee.vn/product-i.1.2",
                job_id="my-specific-job-id",
            )

        assert result.job_id == "my-specific-job-id"
