"""Tests for sentiment_analyzer — rule-based scoring."""
from shared.models import ScrapedReview
from app.analyzers.sentiment_analyzer import analyze_sentiment, _score_review


class TestScoreReview:
    def test_positive_keywords_score_1(self):
        r = ScrapedReview(rating=5, content="sản phẩm tốt lắm, rất hài lòng")
        assert _score_review(r) == 1.0

    def test_negative_keywords_score_0(self):
        r = ScrapedReview(rating=1, content="hàng kém chất lượng, thất vọng hoàn toàn")
        assert _score_review(r) == 0.0

    def test_no_content_high_rating_is_positive(self):
        r = ScrapedReview(rating=5, content=None)
        assert _score_review(r) == 1.0

    def test_no_content_low_rating_is_negative(self):
        r = ScrapedReview(rating=1, content=None)
        assert _score_review(r) == 0.0

    def test_no_content_no_rating_is_neutral(self):
        r = ScrapedReview(rating=None, content=None)
        assert _score_review(r) == 0.5


class TestAnalyzeSentiment:
    def test_empty_reviews_returns_none(self):
        assert analyze_sentiment([]) is None

    def test_all_positive_returns_1(self, good_reviews):
        score = analyze_sentiment(good_reviews)
        assert score == 1.0

    def test_all_negative_returns_0(self, bad_reviews):
        score = analyze_sentiment(bad_reviews)
        assert score == 0.0

    def test_mixed_returns_fraction(self, good_reviews, bad_reviews):
        mixed = good_reviews + bad_reviews  # 3 good + 3 bad
        score = analyze_sentiment(mixed)
        assert 0.0 < score < 1.0

    def test_score_range_always_0_to_1(self, good_reviews, bad_reviews, fake_reviews):
        for review_set in [good_reviews, bad_reviews, fake_reviews, good_reviews + bad_reviews]:
            score = analyze_sentiment(review_set)
            assert score is None or 0.0 <= score <= 1.0
