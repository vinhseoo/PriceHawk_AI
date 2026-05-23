"""
RabbitMQ Consumer — analysis.request.queue
Receives AnalysisRequestEvent from Catalog Service (Java), runs the full
analysis pipeline, publishes AnalysisResultEvent back.
"""
import logging
from typing import Any

from shared.models import AnalysisRequestEvent
from shared.rabbitmq_client import (
    AbstractConsumer,
    RabbitMQPublisher,
    ANALYSIS_EXCHANGE,
    ANALYSIS_REQUEST_QUEUE,
    ANALYSIS_REQUEST_KEY,
    ANALYSIS_COMPLETED_KEY,
)
from app.services.analysis_service import AnalysisService

logger = logging.getLogger(__name__)


class AnalysisRequestConsumer(AbstractConsumer):
    queue_name = ANALYSIS_REQUEST_QUEUE
    exchange_name = ANALYSIS_EXCHANGE
    routing_key = ANALYSIS_REQUEST_KEY

    def __init__(self, connection, analysis_service: AnalysisService, publisher: RabbitMQPublisher):
        super().__init__(connection)
        self._service = analysis_service
        self._publisher = publisher

    async def handle(self, payload: dict[str, Any]) -> None:
        try:
            # model_validate handles Java camelCase keys via alias_generator
            event = AnalysisRequestEvent.model_validate(payload)
        except Exception as e:
            logger.error(f"Invalid AnalysisRequestEvent payload: {e} — payload={payload}")
            return  # Permanent failure — don't requeue

        logger.info(
            f"Analysis job received: productId={event.product_id} "
            f"listingId={event.seller_listing_id} reviewCount={event.review_count}"
        )

        try:
            result = await self._service.analyze(event)
            await self._publisher.publish(ANALYSIS_EXCHANGE, ANALYSIS_COMPLETED_KEY, result)
            logger.info(
                f"Analysis published: productId={event.product_id} "
                f"trust={result.trust_score}"
            )
        except Exception as e:
            logger.error(
                f"Analysis failed: productId={event.product_id} error={e}",
                exc_info=True,
            )
            raise  # Re-raise → nack → dead-letter queue
