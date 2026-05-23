package com.pricehawk.catalog.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductDTO(
    UUID id,
    String name,
    String slug,
    String brand,
    String description,
    String thumbnailUrl,
    CategoryDTO category,
    String aiSummary,
    BigDecimal sentimentScore,
    int totalReviews,
    BigDecimal realReviewRatio,
    BigDecimal lowestPrice,
    String lowestPriceSeller,
    String lowestPriceSource,
    List<SellerListingDTO> listings,
    Instant createdAt,
    Instant updatedAt
) {}
