"""Tests for AnalysisRequestConsumer."""
import pytest
from unittest.mock import AsyncMock, MagicMock

from shared.models import AnalysisResultEvent, AnalysisRequestEvent
from shared.rabbitmq_client import ANALYSIS_EXCHANGE, ANALYSIS_COMPLETED_KEY
from app.consumer.analysis_consumer import AnalysisRequestConsumer


def _make_consumer(analysis_service, publisher):
    conn = MagicMock()
    return AnalysisRequestConsumer(conn, analysis_service, publisher)


def _make_result() -> AnalysisResultEvent:
    return AnalysisResultEvent(
        job_id="job-1",
        product_id="prod-1",
        seller_listing_id="listing-1",
        trust_score=0.78,
        total_reviews=50,
    )


class TestAnalysisRequestConsumer:
    @pytest.mark.asyncio
    async def test_handle_valid_payload_publishes_result(self):
        """Java camelCase payload → parsed → analyzed → result published."""
        mock_result = _make_result()
        mock_service = MagicMock()
        mock_service.analyze = AsyncMock(return_value=mock_result)
        mock_publisher = MagicMock()
        mock_publisher.publish = AsyncMock()

        consumer = _make_consumer(mock_service, mock_publisher)

        # Java sends camelCase keys
        payload = {
            "productId": "prod-1",
            "sellerListingId": "listing-1",
            "reviewCount": 50,
        }
        await consumer.handle(payload)

        mock_service.analyze.assert_awaited_once()
        mock_publisher.publish.assert_awaited_once_with(
            ANALYSIS_EXCHANGE,
            ANALYSIS_COMPLETED_KEY,
            mock_result,
        )

    @pytest.mark.asyncio
    async def test_handle_invalid_payload_does_not_publish(self):
        """Malformed payload → returns without publishing (permanent failure)."""
        mock_service = MagicMock()
        mock_service.analyze = AsyncMock()
        mock_publisher = MagicMock()
        mock_publisher.publish = AsyncMock()

        consumer = _make_consumer(mock_service, mock_publisher)
        await consumer.handle({"bad_field": "value"})  # Missing required product_id

        mock_service.analyze.assert_not_awaited()
        mock_publisher.publish.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_analysis_failure_reraises(self):
        """Analyzer exception → re-raised → message nacked."""
        mock_service = MagicMock()
        mock_service.analyze = AsyncMock(side_effect=RuntimeError("Redis timeout"))
        mock_publisher = MagicMock()
        mock_publisher.publish = AsyncMock()

        consumer = _make_consumer(mock_service, mock_publisher)

        with pytest.raises(RuntimeError, match="Redis timeout"):
            await consumer.handle({"productId": "p1", "sellerListingId": "l1", "reviewCount": 0})

        mock_publisher.publish.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_camelcase_fields_parsed_correctly(self):
        """Verify Java's camelCase keys map to snake_case Python fields."""
        captured_event = None

        async def capture_analyze(event: AnalysisRequestEvent):
            nonlocal captured_event
            captured_event = event
            return _make_result()

        mock_service = MagicMock()
        mock_service.analyze = capture_analyze
        mock_publisher = MagicMock()
        mock_publisher.publish = AsyncMock()

        consumer = _make_consumer(mock_service, mock_publisher)
        await consumer.handle({
            "productId": "prod-abc",
            "sellerListingId": "listing-xyz",
            "reviewCount": 42,
        })

        assert captured_event.product_id == "prod-abc"
        assert captured_event.seller_listing_id == "listing-xyz"
        assert captured_event.review_count == 42
