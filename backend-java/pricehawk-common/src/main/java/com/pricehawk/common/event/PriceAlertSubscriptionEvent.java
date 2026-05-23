package com.pricehawk.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by User Service when a user adds/removes a wishlist item with a target price.
 * Consumed by Notification Service to maintain the price-alert subscription store.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlertSubscriptionEvent {

    private String userId;

    /** User's email — included so Notification Service can send email digests. */
    private String userEmail;

    private String productId;

    /**
     * The price the user wants to be alerted at (inclusive).
     * Null if user is adding without a target (still stored, no alert until price drops).
     */
    private BigDecimal targetPrice;

    /** true = subscribe / update target, false = unsubscribe */
    private boolean active;

    @Builder.Default
    private Instant subscribedAt = Instant.now();
}
