"""
RabbitMQ Consumer — scrape.request.queue
Receives ScrapeRequestEvent from Catalog Service, dispatches to orchestrator,
publishes ScrapeResultEvent back via product.scraped routing key.
"""
import logging
import uuid
from typing import Any

from shared.models import ScrapeRequestEvent
from shared.rabbitmq_client import (
    AbstractConsumer,
    RabbitMQPublisher,
    SCRAPE_EXCHANGE,
    SCRAPE_REQUEST_QUEUE,
    SCRAPE_REQUEST_KEY,
    PRODUCT_SCRAPED_KEY,
)

logger = logging.getLogger(__name__)


class ScrapeRequestConsumer(AbstractConsumer):
    queue_name = SCRAPE_REQUEST_QUEUE
    exchange_name = SCRAPE_EXCHANGE
    routing_key = SCRAPE_REQUEST_KEY

    def __init__(self, connection, orchestrator, publisher: RabbitMQPublisher):
        super().__init__(connection)
        self._orchestrator = orchestrator
        self._publisher = publisher

    async def handle(self, payload: dict[str, Any]) -> None:
        try:
            event = ScrapeRequestEvent(**payload)
        except Exception as e:
            logger.error(f"Invalid ScrapeRequestEvent payload: {e} — payload={payload}")
            return  # Permanent failure — don't requeue

        job_id = event.job_id
        url = event.url
        logger.info(f"Scrape job received: jobId={job_id} url={url}")

        await self._set_job_status(job_id, "IN_PROGRESS")

        try:
            result = await self._orchestrator.scrape(
                url=url,
                job_id=job_id,
                discover_sellers=event.discover_sellers,
            )

            # Publish scraped result to Catalog Service consumer
            await self._publisher.publish(SCRAPE_EXCHANGE, PRODUCT_SCRAPED_KEY, result)

            await self._set_job_status(job_id, "COMPLETED")
            logger.info(f"Scrape job completed: jobId={job_id} product={result.product_data.name}")

        except Exception as e:
            logger.error(f"Scrape job failed: jobId={job_id} error={e}", exc_info=True)
            await self._set_job_status(job_id, "FAILED", str(e))
            raise  # Re-raise → message is nacked → goes to retry/DLQ

    async def _set_job_status(self, job_id: str, status: str, error: str | None = None) -> None:
        """Update scrape_jobs table. Uses own DB session — not FastAPI Depends context."""
        try:
            from app.config.database import AsyncSessionLocal
            from app.services.job_service import JobService
            async with AsyncSessionLocal() as db:
                await JobService(db).update_status(uuid.UUID(job_id), status, error)
        except Exception as e:
            # Non-fatal — job status is observability, not correctness
            logger.warning(f"Could not update job status ({job_id} → {status}): {e}")
