"""Tests for AnalysisService — full pipeline orchestration."""
import pytest
from unittest.mock import AsyncMock, MagicMock

from shared.models import AnalysisRequestEvent, ScrapedReview
from app.services.analysis_service import AnalysisService


def _make_event(reviews=None, review_count=None, avg_rating=None, is_official=False) -> AnalysisRequestEvent:
    return AnalysisRequestEvent(
        job_id="job-test",
        product_id="prod-1",
        seller_listing_id="listing-1",
        reviews=reviews or [],
        review_count=review_count or (len(reviews) if reviews else 0),
        avg_rating=avg_rating,
        is_official_store=is_official,
    )


class TestAnalysisServicePipeline:
    @pytest.mark.asyncio
    async def test_empty_reviews_produces_valid_result(self):
        service = AnalysisService(llm_client=None)
        event = _make_event()
        result = await service.analyze(event)

        assert result.product_id == "prod-1"
        assert result.seller_listing_id == "listing-1"
        assert 0.0 <= result.trust_score <= 1.0
        assert result.sentiment_score is None
        assert result.real_review_ratio is None

    @pytest.mark.asyncio
    async def test_good_reviews_high_trust(self, good_reviews):
        service = AnalysisService(llm_client=None)
        event = _make_event(reviews=good_reviews, avg_rating=4.8, is_official=True)
        result = await service.analyze(event)

        assert result.trust_score >= 0.6
        assert result.sentiment_score == 1.0  # all good reviews
        assert result.real_review_ratio == pytest.approx(1.0)

    @pytest.mark.asyncio
    async def test_fake_reviews_reduce_real_ratio(self, fake_reviews):
        service = AnalysisService(llm_client=None)
        event = _make_event(reviews=fake_reviews, avg_rating=5.0)
        result = await service.analyze(event)

        assert result.real_review_ratio is not None
        assert result.real_review_ratio < 1.0  # Some reviews flagged as fake

    @pytest.mark.asyncio
    async def test_llm_summarizer_called_when_available(self, good_reviews):
        mock_llm = MagicMock()
        mock_summarizer_output = {
            "top_pros": ["Pin trâu", "Camera đẹp"],
            "top_cons": ["Giá cao"],
            "recommendation": "Nên mua",
            "summary": "Sản phẩm tốt",
        }

        service = AnalysisService(llm_client=mock_llm)
        # Mock the summarizer directly on the service
        service._summarizer = MagicMock()
        service._summarizer.summarize = AsyncMock(return_value=mock_summarizer_output)

        event = _make_event(reviews=good_reviews)
        result = await service.analyze(event)

        service._summarizer.summarize.assert_awaited_once()
        assert result.top_pros == ["Pin trâu", "Camera đẹp"]
        assert result.recommendation == "Nên mua"
        assert result.ai_summary == "Sản phẩm tốt"

    @pytest.mark.asyncio
    async def test_llm_summarizer_not_called_when_none(self, good_reviews):
        service = AnalysisService(llm_client=None)
        event = _make_event(reviews=good_reviews)
        result = await service.analyze(event)

        # Should degrade gracefully with empty summary
        assert result.top_pros == []
        assert result.top_cons == []
        assert result.recommendation == "Chưa có đánh giá"

    @pytest.mark.asyncio
    async def test_summarizer_failure_does_not_crash(self, good_reviews):
        """LLM exception should be caught — pipeline still returns valid result."""
        mock_llm = MagicMock()
        service = AnalysisService(llm_client=mock_llm)
        service._summarizer = MagicMock()
        service._summarizer.summarize = AsyncMock(side_effect=RuntimeError("LLM timeout"))

        event = _make_event(reviews=good_reviews)
        result = await service.analyze(event)  # Must not raise

        assert result.product_id == "prod-1"
        assert result.top_pros == []  # Graceful default

    @pytest.mark.asyncio
    async def test_review_count_from_java_used_over_len_reviews(self):
        """Java may report 500 total reviews but only send first 100."""
        service = AnalysisService(llm_client=None)
        reviews = [ScrapedReview(rating=5, content="Tốt") for _ in range(20)]
        event = _make_event(reviews=reviews, review_count=500)
        result = await service.analyze(event)

        assert result.total_reviews == 500  # Uses Java's count, not len(reviews)

    @pytest.mark.asyncio
    async def test_result_contains_correct_ids(self, good_reviews):
        service = AnalysisService(llm_client=None)
        event = _make_event(reviews=good_reviews)
        result = await service.analyze(event)

        assert result.job_id == "job-test"
        assert result.product_id == "prod-1"
        assert result.seller_listing_id == "listing-1"
