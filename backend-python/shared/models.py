"""
Pydantic event models — must mirror Java DTOs in pricehawk-common exactly.

Serialization contract:
  Java publishes camelCase JSON (Jackson default).
  Python publishes camelCase JSON (model_dump_json(by_alias=True) via RabbitMQPublisher).
  All models use alias_generator=to_camel so camelCase keys are accepted on input
  and produced on output, while internal Python code uses snake_case field names.
"""
import uuid
from datetime import datetime, timezone
from enum import Enum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


def _now_utc() -> datetime:
    return datetime.now(timezone.utc)


class _Base(BaseModel):
    """Base for all shared models — camelCase JSON in, camelCase JSON out."""
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,   # also accept snake_case field names in Python code
    )


# ─── Enums ────────────────────────────────────────────────────────────────────

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

class ScrapedReview(_Base):
    reviewer_name: str | None = None
    rating: int | None = None
    content: str | None = None
    review_date: str | None = None


class ScrapedSellerListing(_Base):
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


class ScrapedProductData(_Base):
    name: str
    brand: str | None = None
    description: str | None = None
    thumbnail_url: str | None = None
    image_urls: list[str] = Field(default_factory=list)
    specs: dict[str, Any] = Field(default_factory=dict)


class ScrapeResultEvent(_Base):
    job_id: str
    domain: str
    platform: str = "OTHER"
    source_type: SourceType = SourceType.UNKNOWN
    scraper_tier: ScraperTier
    product_data: ScrapedProductData
    seller_listings: list[ScrapedSellerListing] = Field(default_factory=list)
    scraped_at: datetime = Field(default_factory=_now_utc)


class ScrapeRequestEvent(_Base):
    # Java publishes: {jobId, url, userId, requestedAt}
    job_id: str
    url: str
    user_id: str | None = None       # Java: userId (nullable)
    discover_sellers: bool = False   # Python-internal; not sent by Java (defaults False)


# ─── Analysis Events ──────────────────────────────────────────────────────────

class AnalysisRequestEvent(_Base):
    # Java publishes: {productId, sellerListingId, reviewCount, requestedAt}
    # job_id is optional because Java doesn't include it; generated if absent
    job_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    product_id: str
    seller_listing_id: str
    review_count: int = 0            # Java: reviewCount
    reviews: list[ScrapedReview] = Field(default_factory=list)  # Python-to-Python only
    # Optional context for trust score (not sent by Java)
    avg_rating: float | None = None
    is_official_store: bool = False
    current_price: float | None = None


class AnalysisResultEvent(_Base):
    # Python publishes → Java consumes
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
    analyzed_at: datetime = Field(default_factory=_now_utc)


# ─── Scraper Config (Python-internal only, no Java interop) ───────────────────

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
