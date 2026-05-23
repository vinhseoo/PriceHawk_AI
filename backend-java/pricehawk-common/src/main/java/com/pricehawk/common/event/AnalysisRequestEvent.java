package com.pricehawk.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequestEvent {

    /** ID of the Product to analyze */
    private String productId;

    /** ID of the SellerListing whose reviews need analysis */
    private String sellerListingId;

    /** Total number of reviews available for analysis */
    private int reviewCount;

    @Builder.Default
    private Instant requestedAt = Instant.now();
}
