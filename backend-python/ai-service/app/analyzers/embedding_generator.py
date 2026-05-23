"""
Embedding generator — OpenAI text-embedding-3-small (1536 dimensions).
Caches embeddings in Redis (24h TTL) to avoid redundant API calls.
"""
import json
import logging

from app.llm.base_client import SemanticCache

logger = logging.getLogger(__name__)

EMBEDDING_MODEL = "text-embedding-3-small"
EMBEDDING_DIMS = 1536
MAX_INPUT_CHARS = 8000  # ~2000 tokens


class EmbeddingGenerator:
    def __init__(self, openai_client=None):
        """
        openai_client: AsyncOpenAI instance (injected for testability).
        If None, generation is disabled (returns empty list).
        """
        self._client = openai_client
        self._cache = SemanticCache(ttl_seconds=86400)  # 24h for embeddings

    async def generate(self, text: str) -> list[float]:
        """
        Generate embedding for text. Returns empty list if client not available.
        """
        if not self._client:
            logger.warning("EmbeddingGenerator: no OpenAI client — returning empty embedding")
            return []

        truncated = text[:MAX_INPUT_CHARS]
        cache_key = f"embed:{truncated}"

        cached = await self._cache.get(cache_key)
        if cached:
            logger.debug("Embedding cache HIT")
            return json.loads(cached)

        try:
            response = await self._client.embeddings.create(
                model=EMBEDDING_MODEL,
                input=truncated,
            )
            embedding = response.data[0].embedding
            await self._cache.set(cache_key, json.dumps(embedding))
            logger.debug(f"Embedding generated: {len(embedding)} dims")
            return embedding
        except Exception as e:
            logger.error(f"Embedding generation failed: {e}")
            return []
