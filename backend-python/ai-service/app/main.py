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
    yield
    logger.info("Shutting down AI Analyzer Service...")


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
