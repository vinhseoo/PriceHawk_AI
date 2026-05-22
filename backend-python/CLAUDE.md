# Python Backend — CLAUDE.md

## Service Structure

Both Python services share `backend-python/shared/` — import as `from shared.xxx import yyy`.

```
backend-python/
├── shared/          ← NEVER import services into shared
├── scraper-service/ ← port 8083, has PostgreSQL (scraper_db)
└── ai-service/      ← port 8084, Redis cache only
```

## Run Commands

```bash
# From service directory (scraper-service or ai-service)
cd backend-python/scraper-service

# Create venv (first time)
python -m venv .venv
source .venv/bin/activate  # Linux/Mac
.venv\Scripts\activate     # Windows

# Install deps
pip install -r requirements.txt

# Run dev server
uvicorn app.main:app --reload --port 8083

# Run Alembic migration
alembic upgrade head

# Generate new migration
alembic revision --autogenerate -m "add table xyz"
```

## Architecture Pattern

```
FastAPI Router → Service → Repository (SQLAlchemy)
      ↓               ↓
  Pydantic         ORM Model
  Schemas          (never leaked out)
```

- Router: validate via Pydantic, call service, return schema. Zero logic.
- Service: async business logic. Never return SQLAlchemy models directly.
- All I/O uses Pydantic models.

## Required Patterns

**FastAPI route:**
```python
@router.post("/url", response_model=ScrapeJobResponse, status_code=202)
async def trigger_scrape(request: ScrapeURLRequest, service: ScraperService = Depends(get_scraper_service)):
    return await service.create_job(request)
```

**Scraper classes — always extend BaseScraper:**
```python
class MyScraper(BaseScraper):
    async def scrape_product(self, url: str) -> ScrapedProductData:
        ...
```

**LLM calls — always use semantic cache first:**
```python
result = await llm_client.chat_cached(prompt=..., cache_key=hash(prompt))
```

**Settings — always from shared.config:**
```python
from shared.config import settings
db_url = settings.SCRAPER_DB_URL
```

**Alembic migrations:** `alembic/versions/` — never edit existing migration files.

## RabbitMQ Consumer Pattern

```python
class ScrapeConsumer(AbstractConsumer):
    queue = MessageQueueConstants.SCRAPE_REQUEST_QUEUE

    async def handle(self, message: ScrapeRequestEvent):
        await self.orchestrator.scrape(message.url)
```

## Shared Models

`shared/models.py` contains Pydantic models that mirror Java DTOs exactly.
When Java event DTOs change, update Python models immediately.

Key models: `ScrapeResultEvent`, `AnalysisResultEvent`, `ScrapeRequestEvent`.

## Environment Variables

Use `python-dotenv`. All defaults in `shared/config.py`. See `.env.example` at project root.
