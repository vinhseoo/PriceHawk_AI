from abc import ABC, abstractmethod
import httpx
from shared.models import ScrapedProductData, ScrapedSellerListing


class BaseScraper(ABC):
    """All scrapers must extend this class."""

    async def http_get(self, url: str, headers: dict | None = None) -> str:
        default_headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Accept-Language": "vi-VN,vi;q=0.9,en;q=0.8",
        }
        if headers:
            default_headers.update(headers)
        async with httpx.AsyncClient(follow_redirects=True, timeout=30) as client:
            response = await client.get(url, headers=default_headers)
            response.raise_for_status()
            return response.text

    async def playwright_render(self, url: str) -> str:
        from playwright.async_api import async_playwright
        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=True)
            page = await browser.new_page()
            await page.goto(url, wait_until="networkidle", timeout=30000)
            content = await page.content()
            await browser.close()
            return content

    @abstractmethod
    async def scrape_product(self, url: str) -> tuple[ScrapedProductData, list[ScrapedSellerListing]]:
        """Scrape product data and seller listings from URL."""
        pass
