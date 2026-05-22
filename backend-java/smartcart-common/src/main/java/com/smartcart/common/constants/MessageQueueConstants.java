package com.smartcart.common.constants;

public final class MessageQueueConstants {

    private MessageQueueConstants() {}

    // Exchanges
    public static final String SCRAPE_EXCHANGE = "smartcart.scrape";
    public static final String ANALYSIS_EXCHANGE = "smartcart.analysis";
    public static final String PRICE_EXCHANGE = "smartcart.price";

    // Queues
    public static final String SCRAPE_REQUEST_QUEUE = "scrape.request.queue";
    public static final String PRODUCT_SCRAPED_QUEUE = "product.scraped.queue";
    public static final String ANALYSIS_REQUEST_QUEUE = "analysis.request.queue";
    public static final String ANALYSIS_COMPLETED_QUEUE = "analysis.completed.queue";
    public static final String PRICE_UPDATED_QUEUE = "price.updated.queue";
    public static final String SCRAPE_FAILED_QUEUE = "scrape.failed.queue";

    // Routing keys
    public static final String SCRAPE_REQUEST_KEY = "scrape.request";
    public static final String PRODUCT_SCRAPED_KEY = "product.scraped";
    public static final String ANALYSIS_REQUEST_KEY = "analysis.request";
    public static final String ANALYSIS_COMPLETED_KEY = "analysis.completed";
    public static final String PRICE_UPDATED_KEY = "price.updated";
    public static final String SCRAPE_FAILED_KEY = "scrape.failed";
}
