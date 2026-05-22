"""
Trust score: weighted formula per seller listing.
Score range: 0.0 – 1.0 (displayed as X/10 on frontend)
"""
import logging

logger = logging.getLogger(__name__)

WEIGHTS = {
    "avg_rating": 0.30,       # Average star rating (normalized to 0-1)
    "real_review_ratio": 0.25, # % of real reviews
    "review_volume": 0.15,     # Log-scaled review count
    "is_official": 0.20,       # Official store bonus
    "price_reasonableness": 0.10,  # Price not suspiciously low
}


def calculate_trust_score(
    avg_rating: float | None,
    review_count: int,
    fake_ratio: float,
    is_official_store: bool,
    current_price: float | None,
    category_avg_price: float | None = None,
) -> float:
    import math

    rating_score = (avg_rating / 5.0) if avg_rating else 0.5
    real_review_score = 1.0 - fake_ratio
    volume_score = min(1.0, math.log10(max(review_count, 1)) / 3.0)  # 1000 reviews = 1.0
    official_score = 1.0 if is_official_store else 0.5

    price_score = 0.7  # Default neutral
    if current_price and category_avg_price and category_avg_price > 0:
        ratio = current_price / category_avg_price
        if 0.7 <= ratio <= 1.3:
            price_score = 1.0
        elif ratio < 0.5:
            price_score = 0.2  # Suspiciously cheap
        else:
            price_score = 0.8

    trust = (
        rating_score * WEIGHTS["avg_rating"]
        + real_review_score * WEIGHTS["real_review_ratio"]
        + volume_score * WEIGHTS["review_volume"]
        + official_score * WEIGHTS["is_official"]
        + price_score * WEIGHTS["price_reasonableness"]
    )

    return round(min(1.0, max(0.0, trust)), 2)
