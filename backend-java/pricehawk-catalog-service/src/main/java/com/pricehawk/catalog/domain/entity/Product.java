package com.pricehawk.catalog.domain.entity;

import com.pricehawk.data.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = false, unique = true, length = 500)
    private String slug;

    @Column(length = 255)
    private String brand;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "sentiment_score", precision = 3, scale = 2)
    private BigDecimal sentimentScore;

    @Column(name = "total_reviews")
    @Builder.Default
    private int totalReviews = 0;

    @Column(name = "real_review_ratio", precision = 3, scale = 2)
    private BigDecimal realReviewRatio;

    @Column(name = "lowest_price", precision = 15, scale = 2)
    private BigDecimal lowestPrice;

    @Column(name = "lowest_price_seller", length = 255)
    private String lowestPriceSeller;

    @Column(name = "lowest_price_source", length = 255)
    private String lowestPriceSource;

    // name_embedding (vector(1536)) is NOT mapped here.
    // Use ProductRepository native queries for vector read/write operations.

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ProductSpec spec;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SellerListing> listings = new ArrayList<>();
}
