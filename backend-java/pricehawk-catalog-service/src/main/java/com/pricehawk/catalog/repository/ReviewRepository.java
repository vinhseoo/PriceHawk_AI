package com.pricehawk.catalog.repository;

import com.pricehawk.catalog.domain.entity.Review;
import com.pricehawk.data.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends BaseRepository<Review> {

    Page<Review> findByListingId(UUID listingId, Pageable pageable);

    // Only real reviews (not flagged as fake) — used as AI summary input
    Page<Review> findByListingIdAndIsLikelyFakeFalse(UUID listingId, Pageable pageable);

    // Dedup check: same review from re-scrape must not be inserted twice (source_review_id added in V3)
    Optional<Review> findByListingIdAndSourceReviewId(UUID listingId, String sourceReviewId);

    boolean existsByListingIdAndSourceReviewId(UUID listingId, String sourceReviewId);

    // For trust score calculation: ratio of fake reviews to total
    @Query("SELECT COUNT(r) FROM Review r WHERE r.listing.id = :listingId AND r.isLikelyFake = true")
    long countFakeByListingId(@Param("listingId") UUID listingId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.listing.id = :listingId")
    long countByListingId(@Param("listingId") UUID listingId);
}
