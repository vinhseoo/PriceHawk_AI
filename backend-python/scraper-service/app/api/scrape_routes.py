from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel, HttpUrl

from app.services.job_service import JobService, get_job_service

router = APIRouter()


class ScrapeURLRequest(BaseModel):
    url: str
    discover_sellers: bool = False


class ScrapeJobResponse(BaseModel):
    job_id: str
    status: str
    message: str


@router.post("/url", response_model=ScrapeJobResponse, status_code=202)
async def trigger_scrape(
    request: ScrapeURLRequest,
    job_service: JobService = Depends(get_job_service),
):
    """Trigger scraping for a single URL. Returns job ID for polling."""
    job = await job_service.create_and_dispatch(url=request.url, discover_sellers=request.discover_sellers)
    return ScrapeJobResponse(job_id=str(job.id), status=job.status, message="Scrape job queued")


@router.get("/jobs/{job_id}")
async def get_job_status(job_id: str, job_service: JobService = Depends(get_job_service)):
    """Poll scrape job status."""
    job = await job_service.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    return job
