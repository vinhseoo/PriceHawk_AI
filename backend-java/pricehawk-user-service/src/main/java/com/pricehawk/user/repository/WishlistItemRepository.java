package com.pricehawk.user.repository;

import com.pricehawk.user.domain.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findAllByWishlistId(UUID wishlistId);

    Optional<WishlistItem> findByWishlistIdAndProductId(UUID wishlistId, UUID productId);

    boolean existsByWishlistIdAndProductId(UUID wishlistId, UUID productId);
}
