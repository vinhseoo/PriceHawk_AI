package com.pricehawk.user.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AddWishlistItemRequest {

    @NotNull
    private UUID productId;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal targetPrice;
}
