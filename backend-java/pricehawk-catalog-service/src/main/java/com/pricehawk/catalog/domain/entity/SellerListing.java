package com.pricehawk.catalog.domain.entity;

import com.pricehawk.catalog.domain.enums.ScrapeStatus;
import com.pricehawk.common.enums.Platform;
import com.pricehawk.common.enums.ScraperTier;
import com.pricehawk.common.enums.SourceType;
import com.pricehawk.data.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seller_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerListing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Domain of the seller site, e.g. "shopee.vn", "lazada.vn", "tiki.vn"
    @Column(nullable = false, length = 255)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    @Builder.Default
    private SourceType sourceType = SourceType.MARKETPLACE;

    @Enumerated(EnumType.STRING)
    @Column(name = "scraper_tier", length = 20)
    private ScraperTier scraperTier;

    // Platform enum — stored via V3 migration (V1 did not have this column)
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Platform platform;

    @Column(name = "seller_name", nullable = false, length = 255)
    private String sellerName;

    @Column(name = "seller_id", length = 255)
    private String sellerId;

    @Column(name = "seller_url", length = 1000)
    private String sellerUrl;

    @Column(name = "is_official_store")
    @Builder.Default
    private boolean isOfficialStore = false;

    // Full product URL on the seller platform
    @Column(name = "external_url", nullable = false, length = 1000)
    private String externalUrl;

    // Seller's own product ID on their platform — used for idempotent upsert.
    // Partial unique index (domain, external_product_id) WHERE NOT NULL enforced in V2 migration.
    @Column(name = "external_product_id", length = 255)
    private String externalProductId;

    @Column(name = "current_price", precision = 15, scale = 2)
    private BigDecimal currentPrice;

    // Original price before discount (nullable — not always available)
    @Column(name = "original_price", precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Column(length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "promotion_info", columnDefinition = "TEXT")
    private String promotionInfo;

    // Trust score computed by AI Analyzer (0.00–1.00)
    @Column(name = "trust_score", precision = 3, scale = 2)
    private BigDecimal trustScore;

    @Column(name = "review_count")
    @Builder.Default
    private int reviewCount = 0;

    // Average star rating from scraped reviews — V1: DECIMAL(2,1), i.e. 4.5
    @Column(name = "average_rating", precision = 2, scale = 1)
    private BigDecimal averageRating;

    @Column(name = "fake_review_ratio", precision = 3, scale = 2)
    private BigDecimal fakeReviewRatio;

    @Enumerated(EnumType.STRING)
    @Column(name = "scrape_status", nullable = false, length = 20)
    @Builder.Default
    private ScrapeStatus scrapeStatus = ScrapeStatus.PENDING;

    @Column(name = "last_scraped_at")
    private Instant lastScrapedAt;

    // Added via V3 migration
    @Column(name = "is_available")
    @Builder.Default
    private boolean isAvailable = true;

    // Added via V3 migration
    @Column(name = "sold_count")
    @Builder.Default
    private int soldCount = 0;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PriceHistory> priceHistory = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
}
