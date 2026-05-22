package com.smartcart.common.event;

import com.smartcart.common.enums.ScraperTier;
import com.smartcart.common.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeResultEvent {
    private String jobId;
    private String domain;
    private String platform;
    private SourceType sourceType;
    private ScraperTier scraperTier;
    private ScrapedProductData productData;
    private List<ScrapedSellerListing> sellerListings;
    @Builder.Default
    private Instant scrapedAt = Instant.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScrapedProductData {
        private String name;
        private String brand;
        private String description;
        private String thumbnailUrl;
        private List<String> imageUrls;
        private Map<String, Object> specs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScrapedSellerListing {
        private String sellerName;
        private String sellerId;
        private String sellerUrl;
        private Boolean isOfficialStore;
        private String externalUrl;
        private String externalProductId;
        private Double currentPrice;
        private Double originalPrice;
        private String currency;
        private String promotionInfo;
        private Integer reviewCount;
        private Double averageRating;
        private List<ScrapedReview> reviews;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScrapedReview {
        private String reviewerName;
        private Integer rating;
        private String content;
        private String reviewDate;
    }
}
