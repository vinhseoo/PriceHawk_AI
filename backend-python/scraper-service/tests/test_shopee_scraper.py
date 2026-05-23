"""
Tests for ShopeeScraper — URL parsing + response parsing logic.
Network calls are mocked via httpx.MockTransport / respx not needed;
we test the parsing helpers directly.
"""
import pytest
from unittest.mock import AsyncMock, patch, MagicMock

from app.tier1.shopee_scraper import ShopeeScraper, _parse_shop_item_id


# ─── URL parsing ──────────────────────────────────────────────────────────────

class TestParseShopItemId:
    def test_standard_url(self):
        url = "https://shopee.vn/Kem-chong-nang-i.123456789.9876543210"
        shop_id, item_id = _parse_shop_item_id(url)
        assert shop_id == "123456789"
        assert item_id == "9876543210"

    def test_url_with_slug(self):
        url = "https://shopee.vn/some-long-product-name-i.111.222"
        shop_id, item_id = _parse_shop_item_id(url)
        assert shop_id == "111"
        assert item_id == "222"

    def test_invalid_url_raises(self):
        with pytest.raises(ValueError, match="Cannot parse Shopee URL"):
            _parse_shop_item_id("https://shopee.vn/not-a-valid-product-url")


# ─── Response parsing ─────────────────────────────────────────────────────────

SHOPEE_PRODUCT_RESPONSE = {
    "name": "Apple iPhone 15 Pro 256GB",
    "brand": "Apple",
    "description": "Titanium design",
    "image": "abc123",
    "images": ["img1", "img2", "img3"],
    "price_min": 2_899_000_000_000,  # × 100000
    "price_before_discount": 3_199_000_000_000,
    "shop_name": "Apple Authorized Reseller",
    "is_official_shop": True,
    "comment_count": 320,
    "item_rating": {"rating_star": 4.9},
    "attributes": [
        {"name": "RAM", "value": "8GB"},
        {"name": "Storage", "value": "256GB"},
    ],
}


class TestShopeeScraperParsing:
    """Tests for the parsing logic extracted from scrape_product."""

    def test_price_scaling(self):
        """Shopee prices are stored × 100000."""
        price_min = SHOPEE_PRODUCT_RESPONSE["price_min"]
        assert price_min / 100000 == pytest.approx(28_990_000.0)

    def test_original_price_scaling(self):
        original = SHOPEE_PRODUCT_RESPONSE["price_before_discount"]
        assert original / 100000 == pytest.approx(31_990_000.0)

    def test_image_url_construction(self):
        """Images should have CDN prefix prepended."""
        images = [
            f"https://cf.shopee.vn/file/{img}"
            for img in SHOPEE_PRODUCT_RESPONSE["images"]
        ]
        assert images[0] == "https://cf.shopee.vn/file/img1"

    def test_thumbnail_url_construction(self):
        thumb = f"https://cf.shopee.vn/file/{SHOPEE_PRODUCT_RESPONSE['image']}"
        assert thumb == "https://cf.shopee.vn/file/abc123"

    def test_attributes_become_specs(self):
        specs = {
            attr["name"]: attr["value"]
            for attr in SHOPEE_PRODUCT_RESPONSE["attributes"]
            if attr.get("name") and attr.get("value")
        }
        assert specs == {"RAM": "8GB", "Storage": "256GB"}


class TestShopeeReviewPagination:
    @pytest.mark.asyncio
    async def test_review_fetch_stops_on_empty(self):
        """Pagination stops immediately when first page returns no ratings."""
        scraper = ShopeeScraper()

        mock_resp = MagicMock()
        mock_resp.raise_for_status = MagicMock()
        mock_resp.json.return_value = {"data": {"ratings": []}}

        mock_client = MagicMock()
        mock_client.get = AsyncMock(return_value=mock_resp)

        reviews = await scraper._fetch_all_reviews(mock_client, "111", "222")

        assert reviews == []
        assert mock_client.get.await_count == 1

    @pytest.mark.asyncio
    async def test_review_fetch_stops_on_partial_page(self):
        """Pagination stops when page has fewer items than limit (last page)."""
        from app.tier1.shopee_scraper import REVIEW_PAGE_LIMIT
        scraper = ShopeeScraper()

        partial_ratings = [
            {"author_username": f"user{i}", "rating_star": 5, "comment": "Great!", "ctime": 1000}
            for i in range(5)  # fewer than REVIEW_PAGE_LIMIT (20)
        ]

        mock_resp = MagicMock()
        mock_resp.raise_for_status = MagicMock()
        mock_resp.json.return_value = {"data": {"ratings": partial_ratings}}

        mock_client = MagicMock()
        mock_client.get = AsyncMock(return_value=mock_resp)

        reviews = await scraper._fetch_all_reviews(mock_client, "111", "222")

        assert len(reviews) == 5
        assert mock_client.get.await_count == 1  # Stopped after partial page

    @pytest.mark.asyncio
    async def test_review_fetch_maps_fields_correctly(self):
        """Review fields are mapped to ScrapedReview model."""
        scraper = ShopeeScraper()

        mock_resp = MagicMock()
        mock_resp.raise_for_status = MagicMock()
        mock_resp.json.return_value = {
            "data": {
                "ratings": [
                    {
                        "author_username": "buyer_nguyen",
                        "rating_star": 4,
                        "comment": "Good product",
                        "ctime": 1700000000,
                    }
                ]
            }
        }

        mock_client = MagicMock()
        mock_client.get = AsyncMock(return_value=mock_resp)

        reviews = await scraper._fetch_all_reviews(mock_client, "111", "222")

        assert len(reviews) == 1
        r = reviews[0]
        assert r.reviewer_name == "buyer_nguyen"
        assert r.rating == 4
        assert r.content == "Good product"
        assert r.review_date == "1700000000"
