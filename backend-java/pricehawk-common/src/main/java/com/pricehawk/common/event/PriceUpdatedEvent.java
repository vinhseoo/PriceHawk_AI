package com.pricehawk.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdatedEvent {

    private String productId;
    private String productName;
    private String sellerListingId;
    private String domain;
    private String sellerName;
    private BigDecimal previousPrice;
    private BigDecimal newPrice;
    private String currency;

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
