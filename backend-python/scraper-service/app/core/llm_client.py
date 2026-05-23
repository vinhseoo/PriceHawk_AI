"""
LLM client wrapper for Tier 3 AI Generic Scraper.
Supports OpenAI GPT. Falls back gracefully if API key is not set.
"""
import logging
from shared.config import settings

logger = logging.getLogger(__name__)


class LLMClient:
    """Async wrapper around OpenAI chat completions."""

    def __init__(self):
        if not settings.OPENAI_API_KEY:
            logger.warning("OPENAI_API_KEY not set — Tier 3 AI scraper will fail at runtime")
        from openai import AsyncOpenAI
        self._client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
        self._model = settings.OPENAI_MODEL

    async def chat(self, system: str, user: str, response_format: str = "text") -> str:
        """
        Send a chat completion request.
        response_format: "text" (default) or "json" (forces JSON output)
        """
        messages = [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ]
        kwargs: dict = {}
        if response_format == "json":
            kwargs["response_format"] = {"type": "json_object"}

        response = await self._client.chat.completions.create(
            model=self._model,
            messages=messages,
            temperature=0.1,   # Low temperature for deterministic extraction
            max_tokens=2000,
            **kwargs,
        )
        return response.choices[0].message.content or ""
