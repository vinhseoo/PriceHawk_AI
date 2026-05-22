import json
import logging
from abc import ABC, abstractmethod
from typing import Any

import aio_pika
from pydantic import BaseModel

from shared.config import settings

logger = logging.getLogger(__name__)

SCRAPE_EXCHANGE = "smartcart.scrape"
ANALYSIS_EXCHANGE = "smartcart.analysis"
PRICE_EXCHANGE = "smartcart.price"

SCRAPE_REQUEST_QUEUE = "scrape.request.queue"
PRODUCT_SCRAPED_QUEUE = "product.scraped.queue"
ANALYSIS_REQUEST_QUEUE = "analysis.request.queue"
ANALYSIS_COMPLETED_QUEUE = "analysis.completed.queue"
PRICE_UPDATED_QUEUE = "price.updated.queue"

SCRAPE_REQUEST_KEY = "scrape.request"
PRODUCT_SCRAPED_KEY = "product.scraped"
ANALYSIS_REQUEST_KEY = "analysis.request"
ANALYSIS_COMPLETED_KEY = "analysis.completed"
PRICE_UPDATED_KEY = "price.updated"


class RabbitMQPublisher:
    def __init__(self, connection: aio_pika.abc.AbstractConnection):
        self._connection = connection
        self._channel: aio_pika.abc.AbstractChannel | None = None

    async def connect(self):
        self._channel = await self._connection.channel()

    async def publish(self, exchange_name: str, routing_key: str, event: BaseModel | dict):
        if self._channel is None:
            await self.connect()
        exchange = await self._channel.declare_exchange(
            exchange_name, aio_pika.ExchangeType.TOPIC, durable=True
        )
        payload = event.model_dump_json() if isinstance(event, BaseModel) else json.dumps(event)
        message = aio_pika.Message(
            body=payload.encode(),
            content_type="application/json",
            delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
        )
        await exchange.publish(message, routing_key=routing_key)
        logger.debug(f"Published to {exchange_name}/{routing_key}")


class AbstractConsumer(ABC):
    queue_name: str
    exchange_name: str
    routing_key: str

    def __init__(self, connection: aio_pika.abc.AbstractConnection):
        self._connection = connection

    async def start(self):
        channel = await self._connection.channel()
        await channel.set_qos(prefetch_count=10)
        exchange = await channel.declare_exchange(self.exchange_name, aio_pika.ExchangeType.TOPIC, durable=True)
        queue = await channel.declare_queue(self.queue_name, durable=True)
        await queue.bind(exchange, routing_key=self.routing_key)
        await queue.consume(self._process_message)
        logger.info(f"Consumer started for queue: {self.queue_name}")

    async def _process_message(self, message: aio_pika.abc.AbstractIncomingMessage):
        async with message.process():
            try:
                payload = json.loads(message.body)
                await self.handle(payload)
            except Exception as e:
                logger.error(f"Error processing message from {self.queue_name}: {e}", exc_info=True)

    @abstractmethod
    async def handle(self, payload: dict[str, Any]):
        pass


async def create_connection() -> aio_pika.abc.AbstractConnection:
    return await aio_pika.connect_robust(settings.get_rabbitmq_url())
