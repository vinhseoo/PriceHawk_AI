package com.pricehawk.catalog.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceHistoryDTO(
    UUID id,
    BigDecimal price,
    String currency,
    Instant recordedAt
) {}
