package com.pricehawk.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultEvent {
    private String jobId;
    private String productId;
    private String sellerListingId;
    private String aiSummary;
    private Double sentimentScore;
    private Integer totalReviews;
    private Double realReviewRatio;
    private Double trustScore;
    private List<String> topPros;
    private List<String> topCons;
    private String recommendation;
    @Builder.Default
    private Instant analyzedAt = Instant.now();
}
