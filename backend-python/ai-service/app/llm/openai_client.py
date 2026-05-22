import json
import logging
from tenacity import retry, stop_after_attempt, wait_exponential

from openai import AsyncOpenAI
from shared.config import settings
from app.llm.base_client import BaseLLMClient, SemanticCache

logger = logging.getLogger(__name__)


class OpenAIClient(BaseLLMClient):
    def __init__(self):
        self._client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
        self._cache = SemanticCache()
        self._model = settings.OPENAI_MODEL

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
    async def chat(self, system: str, user: str, response_format: str = "text") -> str:
        cache_key = f"{system}|{user}"
        cached = await self._cache.get(cache_key)
        if cached:
            return cached

        kwargs = {"model": self._model, "messages": [{"role": "system", "content": system}, {"role": "user", "content": user}]}
        if response_format == "json":
            kwargs["response_format"] = {"type": "json_object"}

        response = await self._client.chat.completions.create(**kwargs)
        result = response.choices[0].message.content
        await self._cache.set(cache_key, result)
        return result
