"""
Rule-based sentiment analyzer for Vietnamese + English product reviews.
Returns a sentiment_score (0.0–1.0) representing the positive review ratio.
Pure Python — no LLM calls, no external deps — fast and free.
"""
from shared.models import ScrapedReview

# Vietnamese + common English positive/negative keywords for product reviews
_POSITIVE = {
    "tốt", "hay", "đẹp", "nhanh", "chính hãng", "tuyệt", "xuất sắc", "ổn", "hài lòng",
    "thích", "ngon", "chất", "đỉnh", "xịn", "ok", "oke", "okê", "hoàn hảo", "tuyệt vời",
    "recommend", "giao nhanh", "đúng mô tả", "chất lượng", "bền", "tốt lắm", "ưng",
    "good", "great", "excellent", "love", "perfect", "amazing", "satisfied", "fast",
    "genuine", "recommend", "best",
}

_NEGATIVE = {
    "tệ", "kém", "xấu", "chậm", "lỗi", "hỏng", "giả", "không hài lòng", "thất vọng",
    "kém chất lượng", "dởm", "không ổn", "vỡ", "sai", "khác mô tả", "giao nhầm",
    "thất vọng", "tệ hại", "không recommend", "không nên mua", "không đáng tiền",
    "bad", "poor", "broken", "fake", "slow", "disappointed", "wrong", "terrible", "worst",
}


def _score_review(review: ScrapedReview) -> float:
    """Return 1.0 (positive), 0.0 (negative), or 0.5 (neutral) for one review."""
    text = (review.content or "").lower()

    pos_hits = sum(1 for w in _POSITIVE if w in text)
    neg_hits = sum(1 for w in _NEGATIVE if w in text)

    if pos_hits > neg_hits:
        return 1.0
    if neg_hits > pos_hits:
        return 0.0

    # Tie-break with star rating
    if review.rating is not None:
        if review.rating >= 4:
            return 1.0
        if review.rating <= 2:
            return 0.0

    return 0.5  # Truly ambiguous


def analyze_sentiment(reviews: list[ScrapedReview]) -> float | None:
    """
    Compute overall sentiment score for a list of reviews.
    Returns proportion of positive reviews (0.0–1.0), or None if no reviews.
    """
    if not reviews:
        return None
    scores = [_score_review(r) for r in reviews]
    return round(sum(scores) / len(scores), 2)
