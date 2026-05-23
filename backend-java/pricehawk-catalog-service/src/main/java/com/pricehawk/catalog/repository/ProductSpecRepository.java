package com.pricehawk.catalog.repository;

import com.pricehawk.catalog.domain.entity.ProductSpec;
import com.pricehawk.data.repository.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductSpecRepository extends BaseRepository<ProductSpec> {

    Optional<ProductSpec> findByProductId(UUID productId);
}
