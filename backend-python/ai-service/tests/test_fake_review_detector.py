"""Tests for FakeReviewDetector rule-based detection."""
from shared.models import ScrapedReview
from app.analyzers.fake_review_detector import detect_fake_reviews


class TestDetectFakeReviews:
    def test_empty_reviews_returns_zero_ratio(self):
        results, ratio = detect_fake_reviews([])
        assert results == []
        assert ratio == 0.0

    def test_short_5star_flagged(self):
        reviews = [ScrapedReview(rating=5, content="ok")]
        results, ratio = detect_fake_reviews(reviews)
        assert results[0]["is_likely_fake"] is True
        assert results[0]["fake_reason"] == "SHORT_5STAR"
        assert ratio == 1.0

    def test_empty_5star_flagged(self):
        reviews = [ScrapedReview(rating=5, content=None)]
        results, ratio = detect_fake_reviews(reviews)
        assert results[0]["is_likely_fake"] is True
        assert results[0]["fake_reason"] == "EMPTY_5STAR"

    def test_duplicate_content_flagged(self):
        content = "sản phẩm rất tốt đáng mua"
        reviews = [
            ScrapedReview(rating=5, content=content),
            ScrapedReview(rating=5, content=content),
            ScrapedReview(rating=5, content=content),  # 3rd copy triggers DUPLICATE_CONTENT
        ]
        results, ratio = detect_fake_reviews(reviews)
        # All 3 with same content should be flagged
        flagged = [r for r in results if r["is_likely_fake"]]
        assert len(flagged) == 3

    def test_genuine_review_not_flagged(self):
        reviews = [
            ScrapedReview(rating=4, content="Sản phẩm khá tốt, đúng với mô tả, giao hàng nhanh"),
        ]
        results, ratio = detect_fake_reviews(reviews)
        assert results[0]["is_likely_fake"] is False
        assert ratio == 0.0

    def test_mixed_reviews_partial_fake_ratio(self, fake_reviews, good_reviews):
        all_reviews = good_reviews + fake_reviews[:2]  # 3 good + 2 fake
        _results, ratio = detect_fake_reviews(all_reviews)
        assert 0.0 < ratio < 1.0

    def test_fake_ratio_range(self):
        reviews = [
            ScrapedReview(rating=5, content="ok"),  # fake
            ScrapedReview(rating=4, content="Sản phẩm tốt, đúng mô tả, chất lượng ổn"),  # real
        ]
        _, ratio = detect_fake_reviews(reviews)
        assert 0.0 <= ratio <= 1.0
