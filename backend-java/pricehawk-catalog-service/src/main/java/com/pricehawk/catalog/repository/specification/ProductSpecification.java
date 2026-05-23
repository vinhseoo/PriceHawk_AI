package com.pricehawk.catalog.repository.specification;

import com.pricehawk.catalog.domain.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Composable JPA Specifications for dynamic product filtering.
 * Usage: Specification.where(hasCategory(id)).and(hasBrand(brand)).and(priceBetween(min, max))
 */
public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) ->
            categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasBrand(String brand) {
        return (root, query, cb) ->
            brand == null ? null : cb.equal(cb.lower(root.get("brand")), brand.toLowerCase());
    }

    public static Specification<Product> nameLike(String keyword) {
        return (root, query, cb) ->
            keyword == null ? null :
                cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lowestPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lowestPrice"), maxPrice));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> hasPriceData() {
        return (root, query, cb) -> cb.isNotNull(root.get("lowestPrice"));
    }

    public static Specification<Product> sentimentScoreAtLeast(BigDecimal minScore) {
        return (root, query, cb) ->
            minScore == null ? null :
                cb.greaterThanOrEqualTo(root.get("sentimentScore"), minScore);
    }

    public static Specification<Product> realReviewRatioAtLeast(BigDecimal minRatio) {
        return (root, query, cb) ->
            minRatio == null ? null :
                cb.greaterThanOrEqualTo(root.get("realReviewRatio"), minRatio);
    }

    /**
     * Build a composite specification from a filter record.
     * Returns Specification.where(null) if all filter fields are null (matches everything).
     */
    public static Specification<Product> fromFilter(ProductFilter filter) {
        return Specification
            .where(hasCategory(filter.categoryId()))
            .and(hasBrand(filter.brand()))
            .and(nameLike(filter.keyword()))
            .and(priceBetween(filter.minPrice(), filter.maxPrice()))
            .and(filter.requirePriceData() ? hasPriceData() : null)
            .and(sentimentScoreAtLeast(filter.minSentimentScore()))
            .and(realReviewRatioAtLeast(filter.minRealReviewRatio()));
    }

    /**
     * Immutable filter record — build via ProductFilter.builder().
     */
    public record ProductFilter(
        UUID categoryId,
        String brand,
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean requirePriceData,
        BigDecimal minSentimentScore,
        BigDecimal minRealReviewRatio
    ) {
        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private UUID categoryId;
            private String brand;
            private String keyword;
            private BigDecimal minPrice;
            private BigDecimal maxPrice;
            private boolean requirePriceData = false;
            private BigDecimal minSentimentScore;
            private BigDecimal minRealReviewRatio;

            public Builder categoryId(UUID v)              { this.categoryId = v; return this; }
            public Builder brand(String v)                 { this.brand = v; return this; }
            public Builder keyword(String v)               { this.keyword = v; return this; }
            public Builder minPrice(BigDecimal v)          { this.minPrice = v; return this; }
            public Builder maxPrice(BigDecimal v)          { this.maxPrice = v; return this; }
            public Builder requirePriceData(boolean v)     { this.requirePriceData = v; return this; }
            public Builder minSentimentScore(BigDecimal v) { this.minSentimentScore = v; return this; }
            public Builder minRealReviewRatio(BigDecimal v){ this.minRealReviewRatio = v; return this; }

            public ProductFilter build() {
                return new ProductFilter(categoryId, brand, keyword, minPrice, maxPrice,
                    requirePriceData, minSentimentScore, minRealReviewRatio);
            }
        }
    }
}
