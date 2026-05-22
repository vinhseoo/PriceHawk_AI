import uuid
import logging
from datetime import datetime
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from fastapi import Depends

from app.config.database import get_db
from app.models import ScrapeJobModel
from shared.utils import extract_domain

logger = logging.getLogger(__name__)


class JobService:
    def __init__(self, db: AsyncSession):
        self._db = db

    async def create_and_dispatch(self, url: str, discover_sellers: bool = False) -> ScrapeJobModel:
        job = ScrapeJobModel(
            id=uuid.uuid4(),
            url=url,
            domain=extract_domain(url),
            status="PENDING",
            discover_sellers=discover_sellers,
        )
        self._db.add(job)
        await self._db.commit()
        await self._db.refresh(job)

        # TODO: publish to RabbitMQ scrape.request.queue in Phase 4
        logger.info(f"Created scrape job {job.id} for {url}")
        return job

    async def get_job(self, job_id: str) -> ScrapeJobModel | None:
        result = await self._db.execute(select(ScrapeJobModel).where(ScrapeJobModel.id == uuid.UUID(job_id)))
        return result.scalar_one_or_none()

    async def update_status(self, job_id: uuid.UUID, status: str, error: str | None = None):
        job = await self._db.get(ScrapeJobModel, job_id)
        if job:
            job.status = status
            if status == "IN_PROGRESS":
                job.started_at = datetime.utcnow()
            elif status in ("COMPLETED", "FAILED"):
                job.completed_at = datetime.utcnow()
            if error:
                job.error_message = error
            await self._db.commit()


async def get_job_service(db: AsyncSession = Depends(get_db)) -> JobService:
    return JobService(db)
