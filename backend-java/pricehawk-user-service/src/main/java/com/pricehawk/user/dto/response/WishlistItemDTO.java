package com.pricehawk.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WishlistItemDTO {

    private UUID id;
    private UUID productId;
    private BigDecimal targetPrice;
    private Instant createdAt;
}
