class PriceHawkException(Exception):
    """Base exception for PriceHawk services."""
    def __init__(self, message: str, error_code: str = "INTERNAL_ERROR"):
        self.message = message
        self.error_code = error_code
        super().__init__(message)


class ScrapingException(PriceHawkException):
    def __init__(self, message: str, url: str = ""):
        self.url = url
        super().__init__(message, "SCRAPING_ERROR")


class ConfigNotFoundException(PriceHawkException):
    def __init__(self, domain: str):
        super().__init__(f"No scraper config found for domain: {domain}", "CONFIG_NOT_FOUND")


class LLMException(PriceHawkException):
    def __init__(self, message: str):
        super().__init__(message, "LLM_ERROR")
