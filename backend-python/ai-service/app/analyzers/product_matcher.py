"""
Product name matcher — Jaccard / token-overlap similarity.
Used for cross-source deduplication: same physical product from Shopee + TGDĐ
should resolve to one Product record in catalog_db.

No heavy ML deps — pure Python + stdlib.
"""
import math
import re
import logging
from dataclasses import dataclass

logger = logging.getLogger(__name__)

# Common noise tokens to strip before comparison
_NOISE = {
    "chính", "hãng", "mới", "100%", "bảo", "hành", "tháng", "năm",
    "the", "a", "an", "with", "for", "and", "or", "of",
    "mẫu", "màu", "phiên", "bản",
}

# Brand normalization: aliases → canonical
_BRAND_ALIASES: dict[str, str] = {
    "táo": "apple",
    "iphon": "iphone",
    "sam sung": "samsung",
}


def _normalize(name: str) -> set[str]:
    """
    Lowercase → strip punctuation → tokenize → remove noise.
    Returns token set.
    """
    name = name.lower()
    name = re.sub(r"[^\w\s]", " ", name)  # strip punctuation
    tokens = set(name.split()) - _NOISE
    # Apply brand aliases
    result = set()
    for t in tokens:
        result.add(_BRAND_ALIASES.get(t, t))
    return result


def jaccard_similarity(name_a: str, name_b: str) -> float:
    """
    Jaccard similarity over normalized token sets.
    Range: 0.0 (no overlap) – 1.0 (identical).
    """
    set_a = _normalize(name_a)
    set_b = _normalize(name_b)
    if not set_a or not set_b:
        return 0.0
    intersection = set_a & set_b
    union = set_a | set_b
    return len(intersection) / len(union)


@dataclass
class MatchResult:
    candidate_index: int | None
    similarity: float
    matched: bool


class ProductMatcher:
    """
    Finds the best-matching candidate for a query product name.
    Use MATCH_THRESHOLD to control strictness.
    """
    MATCH_THRESHOLD = 0.45  # Tuned empirically — 0.45 catches same product with different SKU descriptors

    def find_best_match(
        self,
        query: str,
        candidates: list[str],
        threshold: float | None = None,
    ) -> MatchResult:
        """
        Returns MatchResult with the best matching candidate index and similarity score.
        If no candidate exceeds threshold, returns MatchResult(candidate_index=None, ..., matched=False).
        """
        if not candidates:
            return MatchResult(candidate_index=None, similarity=0.0, matched=False)

        threshold = threshold if threshold is not None else self.MATCH_THRESHOLD

        best_idx = 0
        best_score = 0.0
        for i, candidate in enumerate(candidates):
            score = jaccard_similarity(query, candidate)
            if score > best_score:
                best_score = score
                best_idx = i

        matched = best_score >= threshold
        if matched:
            logger.debug(f"ProductMatcher: '{query[:40]}' → candidate[{best_idx}] ({best_score:.2f})")
        return MatchResult(
            candidate_index=best_idx if matched else None,
            similarity=round(best_score, 3),
            matched=matched,
        )

    def is_same_product(self, name_a: str, name_b: str, threshold: float | None = None) -> bool:
        """Quick two-name comparison."""
        t = threshold if threshold is not None else self.MATCH_THRESHOLD
        return jaccard_similarity(name_a, name_b) >= t
