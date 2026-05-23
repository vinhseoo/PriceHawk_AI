package com.pricehawk.catalog.repository;

import com.pricehawk.catalog.domain.entity.SellerListing;
import com.pricehawk.catalog.domain.enums.ScrapeStatus;
import com.pricehawk.data.repository.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerListingRepository extends BaseRepository<SellerListing> {

    List<SellerListing> findByProductId(UUID productId);

    Optional<SellerListing> findByDomainAndExternalProductId(String domain, String externalProductId);

    List<SellerListing> findByScrapeStatus(ScrapeStatus status);

    // findFirst + Spring Data derived method — avoids LIMIT in JPQL
    Optional<SellerListing> findFirstByProductIdAndIsAvailableTrueOrderByCurrentPriceAsc(UUID productId);

    boolean existsByDomainAndExternalProductId(String domain, String externalProductId);
}
