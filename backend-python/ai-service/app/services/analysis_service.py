"""
AnalysisService — orchestrates the full review analysis pipeline:
1. Fake review detection (rule-based, free)
2. Sentiment analysis (rule-based, free)
3. Trust score calculation (weighted formula)
4. Review summarization (LLM — skipped if client absent or no reviews)
Returns AnalysisResultEvent ready for publishing.
"""
import logging
from shared.models import AnalysisRequestEvent, AnalysisResultEvent, ScrapedReview
from app.analyzers.fake_review_detector import detect_fake_reviews
from app.analyzers.sentiment_analyzer import analyze_sentiment
from app.analyzers.trust_score_calculator import calculate_trust_score
from app.analyzers.review_summarizer import ReviewSummarizer

logger = logging.getLogger(__name__)

_EMPTY_SUMMARY = {
    "top_pros": [],
    "top_cons": [],
    "recommendation": "Chưa có đánh giá",
    "summary": "",
}


class AnalysisService:
    def __init__(self, llm_client=None):
        self._llm = llm_client
        self._summarizer = ReviewSummarizer(llm_client) if llm_client else None

    async def analyze(self, event: AnalysisRequestEvent) -> AnalysisResultEvent:
        reviews: list[ScrapedReview] = event.reviews

        # ── Step 1: Fake review detection ─────────────────────────────────────
        _flagged, fake_ratio = detect_fake_reviews(reviews)

        # ── Step 2: Sentiment analysis ────────────────────────────────────────
        sentiment_score = analyze_sentiment(reviews)

        # ── Step 3: Derive rating/count from provided data or reviews ─────────
        if event.avg_rating is not None:
            avg_rating = event.avg_rating
        elif reviews:
            rated = [r.rating for r in reviews if r.rating is not None]
            avg_rating = sum(rated) / len(rated) if rated else None
        else:
            avg_rating = None

        # review_count: use Java-provided total (may exceed len(reviews) due to pagination cap)
        review_count = event.review_count or len(reviews)

        # ── Step 4: Trust score ───────────────────────────────────────────────
        trust_score = calculate_trust_score(
            avg_rating=avg_rating,
            review_count=review_count,
            fake_ratio=fake_ratio,
            is_official_store=event.is_official_store,
            current_price=event.current_price,
        )

        # ── Step 5: LLM summarization ─────────────────────────────────────────
        summary_data = _EMPTY_SUMMARY
        if self._summarizer and reviews:
            try:
                summary_data = await self._summarizer.summarize(reviews)
            except Exception as e:
                logger.error(f"Review summarization failed for {event.seller_listing_id}: {e}")

        real_review_ratio = round(1.0 - fake_ratio, 3) if reviews else None

        result = AnalysisResultEvent(
            job_id=event.job_id,
            product_id=event.product_id,
            seller_listing_id=event.seller_listing_id,
            ai_summary=summary_data.get("summary") or None,
            sentiment_score=sentiment_score,
            total_reviews=review_count,
            real_review_ratio=real_review_ratio,
            trust_score=trust_score,
            top_pros=summary_data.get("top_pros", []),
            top_cons=summary_data.get("top_cons", []),
            recommendation=summary_data.get("recommendation"),
        )

        logger.info(
            f"Analysis complete: listingId={event.seller_listing_id} "
            f"trust={trust_score:.2f} sentiment={sentiment_score} "
            f"fakeRatio={fake_ratio:.1%} reviews={review_count}"
        )
        return result
