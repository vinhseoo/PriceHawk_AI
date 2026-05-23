"""Shared fixtures for ai-service tests."""
import asyncio
import pytest
from shared.models import ScrapedReview


@pytest.fixture(scope="session")
def event_loop():
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture
def good_reviews():
    return [
        ScrapedReview(reviewer_name="user1", rating=5, content="Sản phẩm tốt lắm, giao nhanh"),
        ScrapedReview(reviewer_name="user2", rating=4, content="Chất lượng ổn, đúng mô tả"),
        ScrapedReview(reviewer_name="user3", rating=5, content="Hài lòng với sản phẩm này"),
    ]


@pytest.fixture
def bad_reviews():
    return [
        ScrapedReview(reviewer_name="userA", rating=1, content="Hàng kém chất lượng, không đáng tiền"),
        ScrapedReview(reviewer_name="userB", rating=2, content="Sản phẩm tệ, thất vọng"),
        ScrapedReview(reviewer_name="userC", rating=1, content="Hỏng ngay sau 1 tuần dùng"),
    ]


@pytest.fixture
def fake_reviews():
    """Reviews that should trigger fake detection rules."""
    return [
        ScrapedReview(reviewer_name="bot1", rating=5, content="ok"),        # SHORT_5STAR
        ScrapedReview(reviewer_name="bot2", rating=5, content=None),         # EMPTY_5STAR
        ScrapedReview(reviewer_name="bot3", rating=5, content="good"),       # SHORT_5STAR
        ScrapedReview(reviewer_name="dup1", rating=5, content="tốt lắm tốt lắm tốt"),  # will have dup
        ScrapedReview(reviewer_name="dup2", rating=5, content="tốt lắm tốt lắm tốt"),
        ScrapedReview(reviewer_name="dup3", rating=5, content="tốt lắm tốt lắm tốt"),  # DUPLICATE_CONTENT
    ]
