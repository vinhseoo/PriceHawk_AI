"""Pydantic event models — must mirror Java DTOs in smartcart-common exactly."""
from datetime import datetime
from enum import Enum
from typing import Any
from pydantic import BaseModel, Field


class ScraperTier(str, Enum):
    API_BASED = "API_BASED"
    CONFIG_BASED = "CONFIG_BASED"
    AI_GENERIC = "AI_GENERIC"


class SourceType(str, Enum):
    MARKETPLACE = "MARKETPLACE"
    RETAILER = "RETAILER"
    UNKNOWN = "UNKNOWN"


class Platform(str, Enum):
    SHOPEE = "SHOPEE"
    LAZADA = "LAZADA"
    TIKI = "TIKI"
    OTHER = "OTHER"


# ─── Scrape Events ────────────────────────────────────────────────────────────

class ScrapedReview(BaseModel):
    reviewer_name: str | None = None
    rating: int | None = None
    content: str | None = None
    review_date: str | None = None


class ScrapedSellerListing(BaseModel):
    seller_name: str
    seller_id: str | None = None
    seller_url: str | None = None
    is_official_store: bool = False
    external_url: str
    external_product_id: str | None = None
    current_price: float | None = None
    original_price: float | None = None
    currency: str = "VND"
    promotion_info: str | None = None
    review_count: int = 0
    average_rating: float | None = None
    reviews: list[ScrapedReview] = Field(default_factory=list)


class ScrapedProductData(BaseModel):
    name: str
    brand: str | None = None
    description: str | None = None
    thumbnail_url: str | None = None
    image_urls: list[str] = Field(default_factory=list)
    specs: dict[str, Any] = Field(default_factory=dict)


class ScrapeResultEvent(BaseModel):
    job_id: str
    domain: str
    platform: str = "OTHER"
    source_type: SourceType = SourceType.UNKNOWN
    scraper_tier: ScraperTier
    product_data: ScrapedProductData
    seller_listings: list[ScrapedSellerListing] = Field(default_factory=list)
    scraped_at: datetime = Field(default_factory=datetime.utcnow)


class ScrapeRequestEvent(BaseModel):
    job_id: str
    url: str
    requested_by: str | None = None
    discover_sellers: bool = False


# ─── Analysis Events ──────────────────────────────────────────────────────────

class AnalysisRequestEvent(BaseModel):
    job_id: str
    product_id: str
    seller_listing_id: str
    reviews: list[ScrapedReview] = Field(default_factory=list)


class AnalysisResultEvent(BaseModel):
    job_id: str
    product_id: str
    seller_listing_id: str
    ai_summary: str | None = None
    sentiment_score: float | None = None
    total_reviews: int = 0
    real_review_ratio: float | None = None
    trust_score: float | None = None
    top_pros: list[str] = Field(default_factory=list)
    top_cons: list[str] = Field(default_factory=list)
    recommendation: str | None = None
    analyzed_at: datetime = Field(default_factory=datetime.utcnow)


# ─── Scraper Config ───────────────────────────────────────────────────────────

class ScraperConfigStatus(str, Enum):
    ACTIVE = "ACTIVE"
    AI_GENERATED = "AI_GENERATED"
    DISABLED = "DISABLED"


class ScraperConfig(BaseModel):
    id: str
    domain: str
    name: str
    selectors: dict[str, str]
    type: str  # "static" or "dynamic"
    pagination: dict | None = None
    is_active: bool = True
    status: ScraperConfigStatus = ScraperConfigStatus.ACTIVE
