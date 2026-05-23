package com.pricehawk.user.controller;

import com.pricehawk.common.response.ApiResponse;
import com.pricehawk.common.response.PageResponse;
import com.pricehawk.user.dto.request.AddWishlistItemRequest;
import com.pricehawk.user.dto.request.RecordSearchRequest;
import com.pricehawk.user.dto.request.UpdateProfileRequest;
import com.pricehawk.user.dto.response.SearchHistoryDTO;
import com.pricehawk.user.dto.response.UserDTO;
import com.pricehawk.user.dto.response.WishlistItemDTO;
import com.pricehawk.user.service.UserService;
import com.pricehawk.user.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WishlistService wishlistService;

    // ── Profile ──────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(toUUID(userId))));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(toUUID(userId), request)));
    }

    // ── Wishlist ──────────────────────────────────────────────────────────────

    @GetMapping("/me/wishlist")
    public ResponseEntity<ApiResponse<List<WishlistItemDTO>>> getWishlist(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getItems(toUUID(userId))));
    }

    @PostMapping("/me/wishlist")
    public ResponseEntity<ApiResponse<WishlistItemDTO>> addToWishlist(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AddWishlistItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(wishlistService.addItem(toUUID(userId), request)));
    }

    @DeleteMapping("/me/wishlist/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID productId) {
        wishlistService.removeItem(toUUID(userId), productId);
        return ResponseEntity.noContent().build();
    }

    // ── Search History & Rate Limit ───────────────────────────────────────────

    @GetMapping("/me/history")
    public ResponseEntity<ApiResponse<PageResponse<SearchHistoryDTO>>> getHistory(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getSearchHistory(toUUID(userId), page, size)));
    }

    @PostMapping("/me/search-record")
    public ResponseEntity<Void> recordSearch(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody RecordSearchRequest request) {
        userService.recordSearch(toUUID(userId), request);
        return ResponseEntity.ok().build();
    }

    private UUID toUUID(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            // Không nên xảy ra nếu JWT hợp lệ, nhưng fail-safe thay vì 500
            throw com.pricehawk.common.exception.BusinessException.unauthorized(
                    "Invalid user identity in token");
        }
    }
}
