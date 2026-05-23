import asyncio
from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.scrape_routes import router as scrape_router
from app.api.config_routes import router as config_router

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Scraper Service...")

    # Wire RabbitMQ, consumer, scheduler
    from shared.rabbitmq_client import create_connection, RabbitMQPublisher
    from app.consumer.scrape_consumer import ScrapeRequestConsumer
    from app.core.orchestrator import ScraperOrchestrator
    from app.core.llm_client import LLMClient
    from app.scheduler.rescraper import rescrape_loop
    from app.config.database import AsyncSessionLocal

    try:
        connection = await create_connection()
        publisher = RabbitMQPublisher(connection)
        await publisher.connect()

        # Build orchestrator with LLM client (graceful if key absent)
        try:
            llm_client = LLMClient()
        except Exception as e:
            logger.warning(f"LLM client init failed — Tier 3 disabled: {e}")
            llm_client = None

        # ConfigService proxy opens its own session per call
        # (orchestrator outlives any single request context)
        orchestrator = ScraperOrchestrator(
            _SessionedConfigService(AsyncSessionLocal),
            llm_client,
        )

        consumer = ScrapeRequestConsumer(connection, orchestrator, publisher)
        await consumer.start()
        logger.info("RabbitMQ consumer started")

        rescraper_task = asyncio.create_task(rescrape_loop(publisher))
        logger.info("Rescraper background task started")

        app.state.rabbitmq_connection = connection
        app.state.rescraper_task = rescraper_task

    except Exception as e:
        logger.error(f"Failed to connect to RabbitMQ — running without messaging: {e}")

    yield

    # Shutdown
    logger.info("Shutting down Scraper Service...")
    if hasattr(app.state, "rescraper_task"):
        app.state.rescraper_task.cancel()
        try:
            await app.state.rescraper_task
        except asyncio.CancelledError:
            pass
    if hasattr(app.state, "rabbitmq_connection"):
        await app.state.rabbitmq_connection.close()


class _SessionedConfigService:
    """
    ConfigService proxy that opens a fresh AsyncSession for each call.
    Required because the orchestrator outlives a single request context.
    """

    def __init__(self, session_factory):
        self._factory = session_factory

    async def get_active_config(self, domain: str):
        from app.services.config_service import ConfigService
        async with self._factory() as db:
            return await ConfigService(db).get_active_config(domain)

    async def save_ai_suggestion(self, domain: str, name: str, selectors: dict):
        from app.services.config_service import ConfigService
        async with self._factory() as db:
            return await ConfigService(db).save_ai_suggestion(domain, name, selectors)


app = FastAPI(
    title="PriceHawk Scraper Service",
    description="3-tier intelligent web scraping service",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(scrape_router, prefix="/api/v1/scrape", tags=["scrape"])
app.include_router(config_router, prefix="/api/v1/scrape/configs", tags=["scraper-configs"])


@app.get("/actuator/health")
async def health():
    return {"status": "UP", "service": "scraper-service"}
