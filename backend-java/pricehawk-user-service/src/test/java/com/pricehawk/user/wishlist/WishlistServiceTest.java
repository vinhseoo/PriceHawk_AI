package com.pricehawk.user.wishlist;

import com.pricehawk.common.exception.BusinessException;
import com.pricehawk.user.domain.entity.Wishlist;
import com.pricehawk.user.domain.entity.WishlistItem;
import com.pricehawk.user.dto.request.AddWishlistItemRequest;
import com.pricehawk.user.dto.response.WishlistItemDTO;
import com.pricehawk.user.mapper.UserMapper;
import com.pricehawk.user.repository.WishlistItemRepository;
import com.pricehawk.user.repository.WishlistRepository;
import com.pricehawk.user.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock WishlistRepository wishlistRepository;
    @Mock WishlistItemRepository wishlistItemRepository;
    @Mock UserMapper userMapper;

    private WishlistService wishlistService;

    @BeforeEach
    void setUp() {
        wishlistService = new WishlistService(wishlistRepository, wishlistItemRepository, userMapper);
    }

    @Test
    void addItem_newProduct_savesItem() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Wishlist wishlist = Wishlist.builder().id(UUID.randomUUID()).userId(userId).build();
        WishlistItem savedItem = WishlistItem.builder()
                .id(UUID.randomUUID())
                .wishlist(wishlist)
                .productId(productId)
                .targetPrice(BigDecimal.valueOf(999_000))
                .build();
        WishlistItemDTO expectedDTO = WishlistItemDTO.builder()
                .id(savedItem.getId())
                .productId(productId)
                .targetPrice(BigDecimal.valueOf(999_000))
                .build();

        AddWishlistItemRequest req = new AddWishlistItemRequest();
        req.setProductId(productId);
        req.setTargetPrice(BigDecimal.valueOf(999_000));

        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)).thenReturn(false);
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(savedItem);
        when(userMapper.toDTO(savedItem)).thenReturn(expectedDTO);

        WishlistItemDTO result = wishlistService.addItem(userId, req);

        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getTargetPrice()).isEqualByComparingTo("999000");
    }

    @Test
    void addItem_duplicateProduct_throwsConflict() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Wishlist wishlist = Wishlist.builder().id(UUID.randomUUID()).userId(userId).build();

        AddWishlistItemRequest req = new AddWishlistItemRequest();
        req.setProductId(productId);

        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)).thenReturn(true);

        assertThatThrownBy(() -> wishlistService.addItem(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in wishlist");
    }

    @Test
    void removeItem_existingProduct_deletesItem() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Wishlist wishlist = Wishlist.builder().id(UUID.randomUUID()).userId(userId).build();
        WishlistItem item = WishlistItem.builder()
                .id(UUID.randomUUID())
                .wishlist(wishlist)
                .productId(productId)
                .build();

        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId))
                .thenReturn(Optional.of(item));

        wishlistService.removeItem(userId, productId);

        verify(wishlistItemRepository).delete(item);
    }

    @Test
    void removeItem_notInWishlist_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Wishlist wishlist = Wishlist.builder().id(UUID.randomUUID()).userId(userId).build();

        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.removeItem(userId, productId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getItems_noWishlistYet_createsWishlistAndReturnsEmpty() {
        UUID userId = UUID.randomUUID();
        Wishlist newWishlist = Wishlist.builder().id(UUID.randomUUID()).userId(userId).build();

        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(newWishlist);
        when(wishlistItemRepository.findAllByWishlistId(newWishlist.getId())).thenReturn(List.of());

        List<WishlistItemDTO> result = wishlistService.getItems(userId);

        assertThat(result).isEmpty();
        verify(wishlistRepository).save(any(Wishlist.class));
    }
}
