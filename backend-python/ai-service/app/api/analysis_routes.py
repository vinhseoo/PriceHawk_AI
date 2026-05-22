from fastapi import APIRouter, Depends
from pydantic import BaseModel
from shared.models import ScrapedReview, AnalysisResultEvent

router = APIRouter()


class AnalyzeReviewsRequest(BaseModel):
    product_id: str
    seller_listing_id: str
    reviews: list[ScrapedReview]


@router.post("/reviews", response_model=AnalysisResultEvent)
async def analyze_reviews(request: AnalyzeReviewsRequest):
    """Analyze reviews: fake detection + sentiment + summarization + trust score."""
    # TODO: wire up analyzers in Phase 5
    return AnalysisResultEvent(
        job_id="placeholder",
        product_id=request.product_id,
        seller_listing_id=request.seller_listing_id,
        total_reviews=len(request.reviews),
    )


@router.post("/embeddings")
async def generate_embeddings(text: str):
    """Generate text embedding for vector search."""
    # TODO: wire up embedding_generator in Phase 5
    return {"embedding": [], "dimensions": 1536}
