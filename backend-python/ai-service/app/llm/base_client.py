import hashlib
import json
import logging
from abc import ABC, abstractmethod

import redis.asyncio as redis
from shared.config import settings

logger = logging.getLogger(__name__)


class BaseLLMClient(ABC):
    @abstractmethod
    async def chat(self, system: str, user: str, response_format: str = "text") -> str:
        pass


class SemanticCache:
    def __init__(self, ttl_seconds: int = 3600):
        self._redis = redis.from_url(settings.get_redis_url())
        self._ttl = ttl_seconds

    def _make_key(self, prompt: str) -> str:
        return f"llm:cache:{hashlib.sha256(prompt.encode()).hexdigest()}"

    async def get(self, prompt: str) -> str | None:
        key = self._make_key(prompt)
        cached = await self._redis.get(key)
        if cached:
            logger.debug(f"Cache HIT for key {key[:16]}...")
            return cached.decode()
        return None

    async def set(self, prompt: str, result: str):
        key = self._make_key(prompt)
        await self._redis.setex(key, self._ttl, result)
