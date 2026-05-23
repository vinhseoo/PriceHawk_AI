"""
Scheduled re-scraper — runs every 12 hours.
Queries scrape_jobs for completed jobs older than 12h and re-submits them.
The scraper service owns which URLs it has successfully scraped, so
re-scheduling from its own history respects the "no cross-DB calls" rule.
"""
import asyncio
import logging
import uuid
from datetime import datetime, timedelta, timezone

from sqlalchemy import select, distinct

from app.config.database import AsyncSessionLocal
from app.models import ScrapeJobModel
from shared.models import ScrapeRequestEvent
from shared.rabbitmq_client import RabbitMQPublisher, SCRAPE_EXCHANGE, SCRAPE_REQUEST_KEY

logger = logging.getLogger(__name__)

RESCRAPE_INTERVAL_HOURS = 12
# Only re-scrape URLs that haven't been attempted in the last N hours
RESCRAPE_COOLDOWN_HOURS = 12


async def rescrape_loop(publisher: RabbitMQPublisher) -> None:
    """Background task: re-scrape active listings on a 12-hour cycle."""
    logger.info("Rescraper background task started")
    while True:
        try:
            await _run_rescrape_cycle(publisher)
        except Exception as e:
            logger.error(f"Rescraper cycle error: {e}", exc_info=True)
        await asyncio.sleep(RESCRAPE_INTERVAL_HOURS * 3600)


async def _run_rescrape_cycle(publisher: RabbitMQPublisher) -> None:
    """Find stale completed jobs and re-publish ScrapeRequestEvent for each unique URL."""
    cutoff = datetime.now(timezone.utc) - timedelta(hours=RESCRAPE_COOLDOWN_HOURS)

    async with AsyncSessionLocal() as db:
        # Find distinct URLs from completed jobs not scraped since cutoff
        result = await db.execute(
            select(ScrapeJobModel.url, ScrapeJobModel.id)
            .where(
                ScrapeJobModel.status == "COMPLETED",
                ScrapeJobModel.completed_at < cutoff,
            )
            .distinct(ScrapeJobModel.url)
            .order_by(ScrapeJobModel.url, ScrapeJobModel.completed_at.asc())
            .limit(500)  # Cap per cycle to avoid thundering herd
        )
        rows = result.all()

    if not rows:
        logger.debug("Rescraper: no stale jobs found")
        return

    logger.info(f"Rescraper: re-submitting {len(rows)} URLs")

    for url, _ in rows:
        new_job_id = str(uuid.uuid4())
        event = ScrapeRequestEvent(
            job_id=new_job_id,
            url=url,
            discover_sellers=False,
        )
        try:
            await publisher.publish(SCRAPE_EXCHANGE, SCRAPE_REQUEST_KEY, event)
        except Exception as e:
            logger.warning(f"Rescraper: failed to publish for {url}: {e}")

    logger.info(f"Rescraper cycle complete — {len(rows)} jobs re-queued")
