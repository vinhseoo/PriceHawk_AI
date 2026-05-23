package com.pricehawk.catalog.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight DTO for product list/search results — excludes heavy fields
 * (listings, aiSummary, description) to keep response size minimal.
 */
public record ProductSummaryDTO(
    UUID id,
    String name,
    String slug,
    String brand,
    String thumbnailUrl,
    String categoryName,
    BigDecimal lowestPrice,
    String lowestPriceSeller,
    String lowestPriceSource,
    BigDecimal sentimentScore,
    BigDecimal realReviewRatio,
    int totalReviews,
    Instant createdAt
) {}
