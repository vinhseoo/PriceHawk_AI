from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.scrape_routes import router as scrape_router
from app.api.config_routes import router as config_router
from app.config.database import engine, Base

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Scraper Service...")
    # DB tables created via Alembic, not here
    yield
    logger.info("Shutting down Scraper Service...")


app = FastAPI(
    title="SmartCart Scraper Service",
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
