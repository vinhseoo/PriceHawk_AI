package com.pricehawk.catalog.repository;

import com.pricehawk.catalog.domain.entity.PriceHistory;
import com.pricehawk.data.repository.BaseRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PriceHistoryRepository extends BaseRepository<PriceHistory> {

    // Most recent prices first — used for price chart rendering in frontend
    List<PriceHistory> findByListingIdOrderByRecordedAtDesc(UUID listingId, Pageable pageable);

    // Chronological order — used for analytics (min/max/avg over time)
    List<PriceHistory> findByListingIdOrderByRecordedAtAsc(UUID listingId);
}
