package com.pricehawk.catalog.dto.response;

import com.pricehawk.catalog.domain.enums.ScrapeStatus;
import com.pricehawk.common.enums.Platform;
import com.pricehawk.common.enums.SourceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SellerListingDTO(
    UUID id,
    String domain,
    SourceType sourceType,
    Platform platform,
    String sellerName,
    boolean isOfficialStore,
    String externalUrl,
    BigDecimal currentPrice,
    BigDecimal originalPrice,
    String currency,
    String promotionInfo,
    BigDecimal trustScore,
    int reviewCount,
    BigDecimal averageRating,
    BigDecimal fakeReviewRatio,
    ScrapeStatus scrapeStatus,
    Instant lastScrapedAt,
    boolean isAvailable,
    int soldCount
) {}
