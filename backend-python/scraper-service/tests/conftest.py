"""
Shared pytest fixtures for scraper-service tests.
"""
import asyncio
import pytest
from unittest.mock import AsyncMock, MagicMock

from shared.models import (
    ScrapedProductData,
    ScrapedSellerListing,
    ScrapedReview,
    ScrapeResultEvent,
    ScrapeRequestEvent,
    ScraperTier,
    SourceType,
)


@pytest.fixture(scope="session")
def event_loop():
    """Single event loop for the entire test session."""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


# ─── Shared model factories ────────────────────────────────────────────────────

@pytest.fixture
def sample_product_data():
    return ScrapedProductData(
        name="iPhone 15 Pro",
        brand="Apple",
        description="Flagship phone",
        image_urls=["https://cdn.example.com/img1.jpg"],
        specs={"RAM": "8GB", "Storage": "256GB"},
    )


@pytest.fixture
def sample_seller_listing(sample_product_data):
    return ScrapedSellerListing(
        seller_name="Apple Store VN",
        seller_id="12345",
        external_url="https://shopee.vn/product-i.12345.67890",
        external_product_id="67890",
        current_price=28_990_000.0,
        original_price=31_990_000.0,
        review_count=150,
        average_rating=4.8,
    )


@pytest.fixture
def sample_scrape_result(sample_product_data, sample_seller_listing):
    return ScrapeResultEvent(
        job_id="test-job-id",
        domain="shopee.vn",
        platform="SHOPEE",
        source_type=SourceType.MARKETPLACE,
        scraper_tier=ScraperTier.API_BASED,
        product_data=sample_product_data,
        seller_listings=[sample_seller_listing],
    )


@pytest.fixture
def mock_publisher():
    publisher = MagicMock()
    publisher.publish = AsyncMock()
    return publisher


@pytest.fixture
def mock_orchestrator(sample_scrape_result):
    orchestrator = MagicMock()
    orchestrator.scrape = AsyncMock(return_value=sample_scrape_result)
    return orchestrator
