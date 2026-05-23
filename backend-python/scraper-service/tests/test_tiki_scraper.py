"""
Tests for TikiScraper — URL parsing + review pagination logic.
"""
import pytest
from unittest.mock import AsyncMock, MagicMock

from app.tier1.tiki_scraper import TikiScraper, _parse_product_id


# ─── URL parsing ──────────────────────────────────────────────────────────────

class TestParseTikiProductId:
    def test_standard_url(self):
        url = "https://tiki.vn/dien-thoai-apple-iphone-15-pro-p123456789.html"
        assert _parse_product_id(url) == "123456789"

    def test_sp_prefix_url(self):
        url = "https://tiki.vn/sp/iphone-15-p987654.html"
        assert _parse_product_id(url) == "987654"

    def test_invalid_url_raises(self):
        with pytest.raises(ValueError, match="Cannot parse Tiki product ID"):
            _parse_product_id("https://tiki.vn/some-page-without-id")


# ─── Review pagination ────────────────────────────────────────────────────────

class TestTikiReviewPagination:
    @pytest.mark.asyncio
    async def test_stops_on_empty_data(self):
        scraper = TikiScraper()

        mock_resp = MagicMock()
        mock_resp.raise_for_status = MagicMock()
        mock_resp.json.return_value = {"data": [], "paging": {"last_page": 1}}

        mock_client = MagicMock()
        mock_client.get = AsyncMock(return_value=mock_resp)

        reviews = await scraper._fetch_all_reviews(mock_client, "123")

        assert reviews == []
        assert mock_client.get.await_count == 1

    @pytest.mark.asyncio
    async def test_stops_at_last_page(self):
        """Pagination respects last_page from paging metadata."""
        scraper = TikiScraper()

        page_1_items = [
            {
                "created_by": {"name": f"user{i}"},
                "rating": 5,
                "content": "Good",
                "created_at": "2024-01-01",
            }
            for i in range(20)
        ]

        call_count = 0

        async def mock_get(url, params=None):
            nonlocal call_count
            call_count += 1
            mock_resp = MagicMock()
            mock_resp.raise_for_status = MagicMock()
            if call_count == 1:
                mock_resp.json.return_value = {
                    "data": page_1_items,
                    "paging": {"last_page": 1},  # Only 1 page
                }
            else:
                mock_resp.json.return_value = {"data": [], "paging": {"last_page": 1}}
            return mock_resp

        mock_client = MagicMock()
        mock_client.get = mock_get

        reviews = await scraper._fetch_all_reviews(mock_client, "123")

        assert len(reviews) == 20
        assert call_count == 1  # Stopped after page 1 (last_page=1)

    @pytest.mark.asyncio
    async def test_review_fields_mapped_correctly(self):
        scraper = TikiScraper()

        mock_resp = MagicMock()
        mock_resp.raise_for_status = MagicMock()
        mock_resp.json.return_value = {
            "data": [
                {
                    "created_by": {"name": "Nguyen Van A"},
                    "rating": 4,
                    "content": "Sản phẩm tốt",
                    "created_at": "2024-03-15",
                }
            ],
            "paging": {"last_page": 1},
        }

        mock_client = MagicMock()
        mock_client.get = AsyncMock(return_value=mock_resp)

        reviews = await scraper._fetch_all_reviews(mock_client, "999")

        assert len(reviews) == 1
        r = reviews[0]
        assert r.reviewer_name == "Nguyen Van A"
        assert r.rating == 4
        assert r.content == "Sản phẩm tốt"
        assert r.review_date == "2024-03-15"

    @pytest.mark.asyncio
    async def test_review_fetch_error_returns_partial(self):
        """If a page fetch fails, accumulated reviews so far are returned."""
        scraper = TikiScraper()

        call_count = 0

        async def mock_get(url, params=None):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                mock_resp = MagicMock()
                mock_resp.raise_for_status = MagicMock()
                mock_resp.json.return_value = {
                    "data": [
                        {"created_by": {"name": "user1"}, "rating": 5,
                         "content": "Nice", "created_at": "2024-01-01"}
                    ],
                    "paging": {"last_page": 3},
                }
                return mock_resp
            else:
                raise ConnectionError("Network error on page 2")

        mock_client = MagicMock()
        mock_client.get = mock_get

        reviews = await scraper._fetch_all_reviews(mock_client, "123")

        # Page 1 reviews preserved; pagination stopped gracefully
        assert len(reviews) == 1
        assert reviews[0].reviewer_name == "user1"
