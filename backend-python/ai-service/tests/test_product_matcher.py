"""Tests for ProductMatcher — Jaccard similarity-based name matching."""
import pytest
from app.analyzers.product_matcher import ProductMatcher, jaccard_similarity


class TestJaccardSimilarity:
    def test_identical_names_score_1(self):
        score = jaccard_similarity("iPhone 15 Pro 256GB", "iPhone 15 Pro 256GB")
        assert score == 1.0

    def test_completely_different_names_score_0_or_low(self):
        score = jaccard_similarity("Samsung Galaxy S24", "Sony WH-1000XM5 Headphones")
        assert score < 0.2

    def test_same_product_different_descriptions_matches(self):
        """Same model from two different stores — should have significant overlap."""
        shopee = "Điện thoại Apple iPhone 15 Pro 256GB chính hãng VNA"
        tgdd = "Apple iPhone 15 Pro 256GB - Hàng chính hãng"
        score = jaccard_similarity(shopee, tgdd)
        assert score >= 0.3  # At minimum: apple, iphone, 15, pro, 256gb, chính, hãng

    def test_empty_name_returns_0(self):
        assert jaccard_similarity("", "iPhone 15") == 0.0
        assert jaccard_similarity("iPhone 15", "") == 0.0


class TestProductMatcher:
    @pytest.fixture
    def matcher(self):
        return ProductMatcher()

    def test_exact_match_found(self, matcher):
        result = matcher.find_best_match(
            "iPhone 15 Pro 256GB",
            ["Samsung Galaxy S24", "iPhone 15 Pro 256GB", "Xiaomi 14 Pro"],
        )
        assert result.matched is True
        assert result.candidate_index == 1

    def test_no_match_below_threshold(self, matcher):
        result = matcher.find_best_match(
            "Sony WH-1000XM5 Headphones",
            ["Samsung Galaxy S24", "Xiaomi 14 Pro", "Apple Watch Series 9"],
        )
        assert result.matched is False
        assert result.candidate_index is None

    def test_empty_candidates_returns_no_match(self, matcher):
        result = matcher.find_best_match("iPhone 15", [])
        assert result.matched is False
        assert result.candidate_index is None

    def test_is_same_product_true_for_similar_names(self, matcher):
        assert matcher.is_same_product(
            "Apple iPhone 15 Pro 256GB",
            "iPhone 15 Pro 256GB Apple",
        ) is True

    def test_is_same_product_false_for_different_models(self, matcher):
        assert matcher.is_same_product(
            "iPhone 15 Pro",
            "Samsung Galaxy S24 Ultra",
        ) is False

    def test_similarity_range_0_to_1(self, matcher):
        result = matcher.find_best_match("iPhone 15", ["Samsung", "Xiaomi"])
        assert 0.0 <= result.similarity <= 1.0
