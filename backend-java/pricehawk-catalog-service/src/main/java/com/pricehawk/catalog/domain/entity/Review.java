package com.pricehawk.catalog.domain.entity;

import com.pricehawk.catalog.domain.enums.Sentiment;
import com.pricehawk.data.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "reviews",
    indexes = {
        @Index(name = "idx_review_listing_id", columnList = "seller_listing_id"),
        @Index(name = "idx_review_is_likely_fake", columnList = "is_likely_fake")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_listing_id", nullable = false)
    private SellerListing listing;

    // External review ID from the seller platform — used for dedup on re-scrape (added in V3)
    @Column(name = "source_review_id", length = 500)
    private String sourceReviewId;

    // Title of the review — not in V1, added in V3
    @Column(length = 500)
    private String title;

    // V1 column name is reviewer_name
    @Column(name = "reviewer_name", length = 255)
    private String authorName;

    // V1 column name is content
    @Column(name = "content", columnDefinition = "TEXT")
    private String body;

    // Star rating 1–5 from the seller platform
    @Column(nullable = false)
    @Builder.Default
    private int rating = 5;

    // Original review date from the seller platform
    @Column(name = "review_date")
    private Instant reviewDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sentiment sentiment;

    // Whether AI Analyzer flagged this review as likely fake
    @Column(name = "is_likely_fake", nullable = false)
    @Builder.Default
    private boolean isLikelyFake = false;

    // Short reason from AI classifier (e.g., "repetitive_pattern", "suspicious_account")
    @Column(name = "fake_reason", length = 100)
    private String fakeReason;

    // AI confidence score that this review is fake (0.00–1.00) — added in V3
    @Column(name = "fake_score", precision = 3, scale = 2)
    private BigDecimal fakeScore;

    // Added via V3 migration
    @Column(name = "helpful_count")
    @Builder.Default
    private int helpfulCount = 0;
}
