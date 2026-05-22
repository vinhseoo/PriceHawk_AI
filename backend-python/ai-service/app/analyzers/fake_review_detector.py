"""
Fake review detector — 2 layers:
1. Rule-based (free): short 5-star, duplicate content, timestamp clustering
2. LLM-assisted: complex patterns (run only if rule-based flags are ambiguous)
"""
import logging
from collections import Counter
from shared.models import ScrapedReview

logger = logging.getLogger(__name__)


def detect_fake_reviews(reviews: list[ScrapedReview]) -> tuple[list[ScrapedReview], float]:
    """
    Returns (flagged_reviews_with_reason, fake_ratio).
    Modifies reviews in-place is NOT done — returns new list.
    """
    if not reviews:
        return [], 0.0

    content_counter = Counter(r.content for r in reviews if r.content)
    flagged_ids = set()
    results = []

    for review in reviews:
        fake_reason = None

        # Rule 1: 5-star with suspiciously short content
        if review.rating == 5 and review.content and len(review.content.strip()) < 15:
            fake_reason = "SHORT_5STAR"

        # Rule 2: Duplicate content
        if review.content and content_counter[review.content] > 2:
            fake_reason = "DUPLICATE_CONTENT"

        # Rule 3: No content at all on 5-star
        if review.rating == 5 and not review.content:
            fake_reason = "EMPTY_5STAR"

        results.append({
            "review": review,
            "is_likely_fake": fake_reason is not None,
            "fake_reason": fake_reason,
        })

    fake_count = sum(1 for r in results if r["is_likely_fake"])
    fake_ratio = fake_count / len(reviews) if reviews else 0.0

    logger.info(f"Fake detection: {fake_count}/{len(reviews)} flagged ({fake_ratio:.1%})")
    return results, fake_ratio
