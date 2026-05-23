package com.pricehawk.user.service;

import com.pricehawk.common.constants.MessageQueueConstants;
import com.pricehawk.common.event.PriceAlertSubscriptionEvent;
import com.pricehawk.common.exception.BusinessException;
import com.pricehawk.messaging.publisher.EventPublisher;
import com.pricehawk.user.domain.entity.User;
import com.pricehawk.user.domain.entity.Wishlist;
import com.pricehawk.user.domain.entity.WishlistItem;
import com.pricehawk.user.dto.request.AddWishlistItemRequest;
import com.pricehawk.user.dto.response.WishlistItemDTO;
import com.pricehawk.user.mapper.UserMapper;
import com.pricehawk.user.repository.UserRepository;
import com.pricehawk.user.repository.WishlistItemRepository;
import com.pricehawk.user.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;

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

        WishlistItemDTO saved = userMapper.toDTO(wishlistItemRepository.save(item));

        // Publish subscription only when a target price is set
        if (request.getTargetPrice() != null) {
            publishSubscriptionEvent(userId, request.getProductId().toString(),
                    request.getTargetPrice(), true);
        }

        return saved;
    }

    @Transactional
    public void removeItem(UUID userId, UUID productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        WishlistItem item = wishlistItemRepository
                .findByWishlistIdAndProductId(wishlist.getId(), productId)
                .orElseThrow(() -> BusinessException.notFound("Wishlist item"));
        wishlistItemRepository.delete(item);

        publishSubscriptionEvent(userId, productId.toString(), null, false);
    }

    private void publishSubscriptionEvent(UUID userId, String productId,
                                          BigDecimal targetPrice, boolean active) {
        try {
            String userEmail = userRepository.findById(userId)
                    .map(User::getEmail)
                    .orElse(null);

            PriceAlertSubscriptionEvent event = PriceAlertSubscriptionEvent.builder()
                    .userId(userId.toString())
                    .userEmail(userEmail)
                    .productId(productId)
                    .targetPrice(targetPrice)
                    .active(active)
                    .build();

            eventPublisher.publish(
                    MessageQueueConstants.PRICE_EXCHANGE,
                    MessageQueueConstants.PRICE_ALERT_SUB_KEY,
                    event
            );
        } catch (Exception e) {
            // Non-fatal: subscription event failure must not roll back the wishlist transaction
            log.warn("Failed to publish PriceAlertSubscriptionEvent userId={} productId={}: {}",
                    userId, productId, e.getMessage());
        }
    }

    private Wishlist getOrCreateWishlist(UUID userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseGet(() -> wishlistRepository.save(
                        Wishlist.builder().userId(userId).build()));
    }
}
