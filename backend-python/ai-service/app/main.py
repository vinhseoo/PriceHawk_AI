import asyncio
from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.analysis_routes import router as analysis_router

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting AI Analyzer Service...")

    from shared.rabbitmq_client import create_connection, RabbitMQPublisher
    from app.consumer.analysis_consumer import AnalysisRequestConsumer
    from app.services.analysis_service import AnalysisService

    # Build LLM client — prefer OpenAI, fall back to Anthropic, degrade gracefully
    llm_client = _init_llm_client()

    analysis_service = AnalysisService(llm_client)

    # Store service on app.state so API routes can reuse the same instance
    app.state.analysis_service = analysis_service
    app.state.llm_client = llm_client

    try:
        connection = await create_connection()
        publisher = RabbitMQPublisher(connection)
        await publisher.connect()

        consumer = AnalysisRequestConsumer(connection, analysis_service, publisher)
        await consumer.start()
        logger.info("RabbitMQ consumer started (analysis.request.queue)")

        app.state.rabbitmq_connection = connection

    except Exception as e:
        logger.error(f"Failed to connect to RabbitMQ — running without messaging: {e}")

    yield

    logger.info("Shutting down AI Analyzer Service...")
    if hasattr(app.state, "rabbitmq_connection"):
        await app.state.rabbitmq_connection.close()


def _init_llm_client():
    """Try OpenAI → Anthropic → None. Logs which client is active."""
    from shared.config import settings

    if settings.OPENAI_API_KEY:
        try:
            from app.llm.openai_client import OpenAIClient
            client = OpenAIClient()
            logger.info(f"LLM: OpenAI ({settings.OPENAI_MODEL})")
            return client
        except Exception as e:
            logger.warning(f"OpenAI init failed: {e}")

    if settings.ANTHROPIC_API_KEY:
        try:
            from app.llm.anthropic_client import AnthropicClient
            client = AnthropicClient()
            logger.info(f"LLM: Anthropic ({settings.ANTHROPIC_MODEL})")
            return client
        except Exception as e:
            logger.warning(f"Anthropic init failed: {e}")

    logger.warning("No LLM client available — review summarization disabled")
    return None


app = FastAPI(
    title="PriceHawk AI Analyzer Service",
    description="Review analysis, trust scoring, embeddings",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
app.include_router(analysis_router, prefix="/api/v1/analysis", tags=["analysis"])


@app.get("/actuator/health")
async def health():
    return {"status": "UP", "service": "ai-service"}
