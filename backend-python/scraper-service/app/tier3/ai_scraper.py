"""
Tier 3 — AI Generic Scraper.
Uses Playwright to render any website + LLM to extract product data.
Also auto-generates CSS selectors to promote extracted site to Tier 2.
"""
import json
import logging
import re
from bs4 import BeautifulSoup
from shared.models import ScrapedProductData, ScrapedSellerListing
from shared.utils import truncate_html, extract_domain
from app.core.base_scraper import BaseScraper

logger = logging.getLogger(__name__)

EXTRACT_SYSTEM = "You are a product data extraction expert. Extract data from e-commerce HTML."
EXTRACT_PROMPT = """
Analyze this HTML from an e-commerce product page.
Extract the following fields as JSON (use null if not found):
{{
  "product_name": "product name string",
  "price": price as number in VND (integers only),
  "original_price": original price before discount or null,
  "brand": "brand name or null",
  "specs": {{"key": "value", ...}} (technical specs),
  "image_urls": ["url1", "url2"],
  "seller_name": "shop/website name",
  "promotion_info": "promotions or null"
}}

Only return valid JSON, no explanation.

HTML:
{html}
"""

SELECTOR_PROMPT = """
Given this HTML and the extracted product data, generate reliable CSS selectors.
Return JSON: {{"product_name": "selector", "price": "selector", "original_price": "selector or null",
"images": "selector", "specs": "selector or null"}}

Product data: {product_data}
HTML: {html}
"""


class AIGenericScraper(BaseScraper):
    def __init__(self, llm_client):
        self._llm = llm_client

    async def scrape_product(self, url: str) -> tuple[ScrapedProductData, list[ScrapedSellerListing]]:
        html = await self.playwright_render(url)
        cleaned = self._clean_html(html)
        focused = truncate_html(cleaned, max_chars=15000)

        response = await self._llm.chat(
            system=EXTRACT_SYSTEM,
            user=EXTRACT_PROMPT.format(html=focused),
            response_format="json",
        )

        try:
            data = json.loads(response)
        except json.JSONDecodeError:
            logger.error(f"LLM returned invalid JSON for {url}")
            data = {}

        product_data = ScrapedProductData(
            name=data.get("product_name", "Unknown Product"),
            brand=data.get("brand"),
            image_urls=data.get("image_urls", [])[:8],
            specs=data.get("specs", {}),
        )

        seller_listing = ScrapedSellerListing(
            seller_name=data.get("seller_name", extract_domain(url)),
            external_url=url,
            current_price=data.get("price"),
            original_price=data.get("original_price"),
            promotion_info=data.get("promotion_info"),
        )

        logger.info(f"[Tier3:AI] Scraped: {product_data.name} from {url}")
        return product_data, [seller_listing]

    async def auto_generate_config(self, url: str, product_data: ScrapedProductData, config_service) -> None:
        """Auto-generate Tier 2 CSS selectors and save as AI_GENERATED for admin review."""
        try:
            html = await self.playwright_render(url)
            cleaned = self._clean_html(html)
            focused = truncate_html(cleaned, max_chars=15000)

            response = await self._llm.chat(
                system=EXTRACT_SYSTEM,
                user=SELECTOR_PROMPT.format(
                    product_data=product_data.model_dump_json(),
                    html=focused,
                ),
                response_format="json",
            )
            selectors = json.loads(response)
            domain = extract_domain(url)

            await config_service.save_ai_suggestion(domain=domain, name=domain, selectors=selectors)
            logger.info(f"[Tier3:AI] Auto-generated config suggestion for {domain}")
        except Exception as e:
            logger.warning(f"Failed to auto-generate config for {url}: {e}")

    def _clean_html(self, html: str) -> str:
        """Remove scripts, styles, and noise to reduce token count."""
        soup = BeautifulSoup(html, "lxml")
        for tag in soup.find_all(["script", "style", "nav", "footer", "header", "aside"]):
            tag.decompose()
        return str(soup)
