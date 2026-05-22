import re
from urllib.parse import urlparse


def extract_domain(url: str) -> str:
    """Extract domain from URL. e.g. 'https://shopee.vn/abc' → 'shopee.vn'"""
    parsed = urlparse(url)
    domain = parsed.netloc.lower()
    # Remove www. prefix
    if domain.startswith("www."):
        domain = domain[4:]
    return domain


def clean_price(price_str: str) -> float | None:
    """Parse Vietnamese price string to float. '28.500.000đ' → 28500000.0"""
    if not price_str:
        return None
    cleaned = re.sub(r"[^\d]", "", price_str)
    return float(cleaned) if cleaned else None


def truncate_html(html: str, max_chars: int = 15000) -> str:
    """Truncate HTML for LLM context window."""
    if len(html) <= max_chars:
        return html
    return html[:max_chars]
