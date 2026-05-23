"""Tests for TrustScoreCalculator."""
import pytest
from app.analyzers.trust_score_calculator import calculate_trust_score


class TestCalculateTrustScore:
    def test_score_is_between_0_and_1(self):
        score = calculate_trust_score(
            avg_rating=4.5,
            review_count=200,
            fake_ratio=0.1,
            is_official_store=True,
            current_price=None,
        )
        assert 0.0 <= score <= 1.0

    def test_high_quality_listing_scores_above_0_7(self):
        """Official store, 4.8 stars, 500 reviews, no fakes → high trust."""
        score = calculate_trust_score(
            avg_rating=4.8,
            review_count=500,
            fake_ratio=0.0,
            is_official_store=True,
            current_price=None,
        )
        assert score >= 0.7

    def test_low_quality_listing_scores_below_0_5(self):
        """Non-official, 3 stars, 5 reviews, 80% fakes → low trust."""
        score = calculate_trust_score(
            avg_rating=3.0,
            review_count=5,
            fake_ratio=0.8,
            is_official_store=False,
            current_price=None,
        )
        assert score < 0.5

    def test_none_avg_rating_uses_neutral_default(self):
        """No rating data → should still compute a score without crashing."""
        score = calculate_trust_score(
            avg_rating=None,
            review_count=0,
            fake_ratio=0.0,
            is_official_store=False,
            current_price=None,
        )
        assert 0.0 <= score <= 1.0

    def test_suspiciously_cheap_price_penalizes_score(self):
        """Price at 30% of category avg → price_score low."""
        score_cheap = calculate_trust_score(
            avg_rating=4.5, review_count=100, fake_ratio=0.0,
            is_official_store=True, current_price=100_000, category_avg_price=500_000
        )
        score_normal = calculate_trust_score(
            avg_rating=4.5, review_count=100, fake_ratio=0.0,
            is_official_store=True, current_price=500_000, category_avg_price=500_000
        )
        assert score_cheap < score_normal

    def test_score_is_rounded_to_2_decimals(self):
        score = calculate_trust_score(
            avg_rating=4.0, review_count=50, fake_ratio=0.2,
            is_official_store=False, current_price=None,
        )
        assert score == round(score, 2)
