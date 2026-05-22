"""Anthropic Claude client — used as fallback when OpenAI is unavailable."""
import logging
from tenacity import retry, stop_after_attempt, wait_exponential

from anthropic import AsyncAnthropic
from shared.config import settings
from app.llm.base_client import BaseLLMClient, SemanticCache

logger = logging.getLogger(__name__)


class AnthropicClient(BaseLLMClient):
    def __init__(self):
        self._client = AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
        self._cache = SemanticCache()
        self._model = settings.ANTHROPIC_MODEL

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
    async def chat(self, system: str, user: str, response_format: str = "text") -> str:
        cache_key = f"{system}|{user}"
        cached = await self._cache.get(cache_key)
        if cached:
            return cached

        message = await self._client.messages.create(
            model=self._model,
            max_tokens=2048,
            system=system,
            messages=[{"role": "user", "content": user}],
        )
        result = message.content[0].text
        await self._cache.set(cache_key, result)
        return result
