"""
Tests for ScrapeRequestConsumer.
"""
import uuid
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from shared.models import ScrapeRequestEvent
from shared.rabbitmq_client import SCRAPE_EXCHANGE, PRODUCT_SCRAPED_KEY
from app.consumer.scrape_consumer import ScrapeRequestConsumer


def _make_consumer(mock_orchestrator, mock_publisher):
    """Build a ScrapeRequestConsumer with mocked dependencies."""
    conn = MagicMock()
    consumer = ScrapeRequestConsumer(conn, mock_orchestrator, mock_publisher)
    return consumer


@pytest.mark.asyncio
async def test_handle_success_publishes_result(mock_orchestrator, mock_publisher, sample_scrape_result):
    """Happy path: valid payload → orchestrator called → result published → COMPLETED."""
    consumer = _make_consumer(mock_orchestrator, mock_publisher)

    job_id = str(uuid.uuid4())
    payload = {
        "job_id": job_id,
        "url": "https://shopee.vn/product-i.12345.67890",
        "discover_sellers": False,
    }

    with patch.object(consumer, "_set_job_status", new_callable=AsyncMock) as mock_status:
        await consumer.handle(payload)

    mock_orchestrator.scrape.assert_awaited_once_with(
        url=payload["url"],
        job_id=job_id,
        discover_sellers=False,
    )
    mock_publisher.publish.assert_awaited_once_with(
        SCRAPE_EXCHANGE,
        PRODUCT_SCRAPED_KEY,
        sample_scrape_result,
    )
    # Status transitions: IN_PROGRESS → COMPLETED
    assert mock_status.await_count == 2
    statuses = [call.args[1] for call in mock_status.await_args_list]
    assert statuses == ["IN_PROGRESS", "COMPLETED"]


@pytest.mark.asyncio
async def test_handle_invalid_payload_does_not_raise(mock_orchestrator, mock_publisher):
    """Malformed payload: consumer logs and returns without publishing (no requeue)."""
    consumer = _make_consumer(mock_orchestrator, mock_publisher)

    await consumer.handle({"bad": "data"})  # Missing required fields

    mock_orchestrator.scrape.assert_not_awaited()
    mock_publisher.publish.assert_not_awaited()


@pytest.mark.asyncio
async def test_handle_scraper_failure_sets_failed_status(mock_orchestrator, mock_publisher):
    """Orchestrator raises → status set to FAILED → exception re-raised."""
    mock_orchestrator.scrape = AsyncMock(side_effect=RuntimeError("Connection timeout"))
    consumer = _make_consumer(mock_orchestrator, mock_publisher)

    job_id = str(uuid.uuid4())
    payload = {
        "job_id": job_id,
        "url": "https://shopee.vn/product-i.99999.11111",
        "discover_sellers": False,
    }

    with patch.object(consumer, "_set_job_status", new_callable=AsyncMock) as mock_status:
        with pytest.raises(RuntimeError, match="Connection timeout"):
            await consumer.handle(payload)

    statuses = [call.args[1] for call in mock_status.await_args_list]
    assert "FAILED" in statuses
    mock_publisher.publish.assert_not_awaited()


@pytest.mark.asyncio
async def test_handle_discover_sellers_passed_through(mock_orchestrator, mock_publisher):
    """discover_sellers=True is forwarded to the orchestrator."""
    consumer = _make_consumer(mock_orchestrator, mock_publisher)

    job_id = str(uuid.uuid4())
    payload = {
        "job_id": job_id,
        "url": "https://tiki.vn/product-p123456.html",
        "discover_sellers": True,
    }

    with patch.object(consumer, "_set_job_status", new_callable=AsyncMock):
        await consumer.handle(payload)

    _, call_kwargs = mock_orchestrator.scrape.await_args
    assert call_kwargs.get("discover_sellers") is True


@pytest.mark.asyncio
async def test_set_job_status_db_failure_is_non_fatal(mock_orchestrator, mock_publisher, sample_scrape_result):
    """DB update failure in _set_job_status must not break the main flow."""
    consumer = _make_consumer(mock_orchestrator, mock_publisher)

    job_id = str(uuid.uuid4())
    payload = {
        "job_id": job_id,
        "url": "https://shopee.vn/product-i.12345.67890",
        "discover_sellers": False,
    }

    # Simulate DB being unavailable
    with patch("app.consumer.scrape_consumer.AsyncSessionLocal", side_effect=Exception("DB down")):
        # Should not raise — _set_job_status swallows DB errors
        await consumer.handle(payload)

    # Result still published despite DB failure
    mock_publisher.publish.assert_awaited_once()
