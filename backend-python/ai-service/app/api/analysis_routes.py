"""
Analysis REST API — used for direct HTTP calls from other services
(or admin testing). The main path is via RabbitMQ consumer.
"""
import uuid

from fastapi import APIRouter, Request, HTTPException
from pydantic import BaseModel

from shared.models import ScrapedReview, AnalysisResultEvent, AnalysisRequestEvent

router = APIRouter()


class AnalyzeReviewsRequest(BaseModel):
    product_id: str
    seller_listing_id: str
    reviews: list[ScrapedReview]
    avg_rating: float | None = None
    review_count: int | None = None
    is_official_store: bool = False
    current_price: float | None = None


class EmbeddingRequest(BaseModel):
    text: str


@router.post("/reviews", response_model=AnalysisResultEvent)
async def analyze_reviews(request_body: AnalyzeReviewsRequest, request: Request):
    """
    Analyze reviews synchronously. Used for testing and direct API calls.
    For production, prefer the RabbitMQ consumer path.
    """
    service = request.app.state.analysis_service
    event = AnalysisRequestEvent(
        job_id=str(uuid.uuid4()),
        product_id=request_body.product_id,
        seller_listing_id=request_body.seller_listing_id,
        reviews=request_body.reviews,
        avg_rating=request_body.avg_rating,
        review_count=request_body.review_count or len(request_body.reviews),
        is_official_store=request_body.is_official_store,
        current_price=request_body.current_price,
    )
    return await service.analyze(event)


@router.post("/embeddings")
async def generate_embeddings(request_body: EmbeddingRequest, request: Request):
    """Generate text embedding for vector search (1536-dim via text-embedding-3-small)."""
    from app.analyzers.embedding_generator import EmbeddingGenerator
    from openai import AsyncOpenAI
    from shared.config import settings

    llm_client = request.app.state.llm_client

    openai_client = None
    if settings.OPENAI_API_KEY:
        try:
            openai_client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
        except Exception:
            pass

    generator = EmbeddingGenerator(openai_client)
    embedding = await generator.generate(request_body.text)
    return {"embedding": embedding, "dimensions": len(embedding)}


@router.post("/match")
async def match_products(name: str, candidates: list[str]):
    """
    Return best-matching candidate index and similarity score.
    Used by Catalog Service to deduplicate products across sources.
    """
    from app.analyzers.product_matcher import ProductMatcher
    matcher = ProductMatcher()
    result = matcher.find_best_match(name, candidates)
    return {
        "matched": result.matched,
        "candidate_index": result.candidate_index,
        "similarity": result.similarity,
    }
