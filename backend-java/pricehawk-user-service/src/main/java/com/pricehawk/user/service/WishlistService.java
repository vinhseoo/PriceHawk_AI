package com.pricehawk.user.service;

import com.pricehawk.common.exception.BusinessException;
import com.pricehawk.user.domain.entity.Wishlist;
import com.pricehawk.user.domain.entity.WishlistItem;
import com.pricehawk.user.dto.request.AddWishlistItemRequest;
import com.pricehawk.user.dto.response.WishlistItemDTO;
import com.pricehawk.user.mapper.UserMapper;
import com.pricehawk.user.repository.WishlistItemRepository;
import com.pricehawk.user.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final UserMapper userMapper;

    // Not readOnly — getOrCreateWishlist may INSERT on first call
    @Transactional
    public List<WishlistItemDTO> getItems(UUID userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        return wishlistItemRepository.findAllByWishlistId(wishlist.getId())
                .stream()
                .map(userMapper::toDTO)
                .toList();
    }

    @Transactional
    public WishlistItemDTO addItem(UUID userId, AddWishlistItemRequest request) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        if (wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), request.getProductId())) {
            throw BusinessException.conflict("Product already in wishlist");
        }

        WishlistItem item = WishlistItem.builder()
                .wishlist(wishlist)
                .productId(request.getProductId())
                .targetPrice(request.getTargetPrice())
                .build();

        return userMapper.toDTO(wishlistItemRepository.save(item));
    }

    @Transactional
    public void removeItem(UUID userId, UUID productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        WishlistItem item = wishlistItemRepository
                .findByWishlistIdAndProductId(wishlist.getId(), productId)
                .orElseThrow(() -> BusinessException.notFound("Wishlist item"));
        wishlistItemRepository.delete(item);
    }

    private Wishlist getOrCreateWishlist(UUID userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseGet(() -> wishlistRepository.save(
                        Wishlist.builder().userId(userId).build()));
    }
}
