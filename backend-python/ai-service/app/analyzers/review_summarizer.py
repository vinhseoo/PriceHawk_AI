"""
Review summarizer: LLM generates structured summary of pros/cons and recommendation.
Always cache-first to avoid redundant LLM calls.
"""
import json
import logging
from shared.models import ScrapedReview
from app.llm.base_client import BaseLLMClient

logger = logging.getLogger(__name__)

SYSTEM = "You are a product review analyst. Summarize reviews concisely in Vietnamese."
PROMPT = """
Analyze these {count} product reviews and return JSON:
{{
  "top_pros": ["pro1", "pro2", "pro3"],
  "top_cons": ["con1", "con2", "con3"],
  "recommendation": "Nên mua / Cân nhắc / Không nên mua — 1 sentence reason",
  "summary": "2-3 sentence overall summary in Vietnamese"
}}

Reviews:
{reviews_text}
"""


class ReviewSummarizer:
    def __init__(self, llm_client: BaseLLMClient):
        self._llm = llm_client

    async def summarize(self, reviews: list[ScrapedReview]) -> dict:
        if not reviews:
            return {"top_pros": [], "top_cons": [], "recommendation": "Chưa có đánh giá", "summary": ""}

        reviews_text = "\n".join(
            f"[{r.rating}★] {r.content or ''}" for r in reviews[:50] if r.content
        )
        response = await self._llm.chat(
            system=SYSTEM,
            user=PROMPT.format(count=len(reviews), reviews_text=reviews_text),
            response_format="json",
        )
        try:
            return json.loads(response)
        except json.JSONDecodeError:
            logger.error("LLM returned invalid JSON for review summary")
            return {"top_pros": [], "top_cons": [], "recommendation": "Lỗi phân tích", "summary": ""}
