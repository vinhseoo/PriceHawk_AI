package com.pricehawk.user.profile;

import com.pricehawk.common.exception.BusinessException;
import com.pricehawk.user.domain.entity.QueryType;
import com.pricehawk.user.domain.entity.SubscriptionPlan;
import com.pricehawk.user.domain.entity.User;
import com.pricehawk.user.dto.request.RecordSearchRequest;
import com.pricehawk.user.dto.request.UpdateProfileRequest;
import com.pricehawk.user.dto.response.UserDTO;
import com.pricehawk.user.mapper.UserMapper;
import com.pricehawk.user.repository.SearchHistoryRepository;
import com.pricehawk.user.repository.UserRepository;
import com.pricehawk.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock SearchHistoryRepository searchHistoryRepository;
    @Mock UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, searchHistoryRepository, userMapper);
    }

    // ── profile ───────────────────────────────────────────────────────────────

    @Test
    void updateProfile_patchesOnlyProvidedFields() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .fullName("Old Name")
                .avatarUrl("old-url")
                .roles(Set.of())
                .build();
        user.setId(userId);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("New Name");
        // avatarUrl not set — should not be overwritten

        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toDTO(any(User.class))).thenReturn(
                UserDTO.builder().fullName("New Name").avatarUrl("old-url").build());

        UserDTO result = userService.updateProfile(userId, req);

        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(result.getAvatarUrl()).isEqualTo("old-url");
    }

    @Test
    void getProfile_unknownUser_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
    }

    // ── rate limit ────────────────────────────────────────────────────────────

    @Test
    void recordSearch_freeUserUnderLimit_incrementsCounter() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .subscriptionPlan(SubscriptionPlan.FREE)
                .dailySearchCount(3)
                .dailySearchResetAt(Instant.now())
                .roles(Set.of())
                .build();
        user.setId(userId);

        RecordSearchRequest req = new RecordSearchRequest();
        req.setQueryType(QueryType.TEXT);
        req.setQueryValue("iphone 15");
        req.setResultCount(10);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(searchHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.recordSearch(userId, req);

        assertThat(user.getDailySearchCount()).isEqualTo(4);
        verify(searchHistoryRepository).save(any());
    }

    @Test
    void recordSearch_freeUserAtLimit_throwsForbidden() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .subscriptionPlan(SubscriptionPlan.FREE)
                .dailySearchCount(5)
                .dailySearchResetAt(Instant.now())
                .roles(Set.of())
                .build();
        user.setId(userId);

        RecordSearchRequest req = new RecordSearchRequest();
        req.setQueryType(QueryType.URL);
        req.setQueryValue("https://shopee.vn/product");

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.recordSearch(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("limit reached");
    }

    @Test
    void recordSearch_premiumUser_noLimit() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .subscriptionPlan(SubscriptionPlan.PREMIUM)
                .dailySearchCount(100)
                .dailySearchResetAt(Instant.now())
                .roles(Set.of())
                .build();
        user.setId(userId);

        RecordSearchRequest req = new RecordSearchRequest();
        req.setQueryType(QueryType.TEXT);
        req.setQueryValue("macbook");
        req.setResultCount(5);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(searchHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.recordSearch(userId, req); // Should not throw

        assertThat(user.getDailySearchCount()).isEqualTo(101);
    }

    @Test
    void recordSearch_newDay_resetsCounterAndAllowsSearch() {
        UUID userId = UUID.randomUUID();
        // Simulate reset time from yesterday
        Instant yesterday = Instant.now().minusSeconds(86_401);
        User user = User.builder()
                .subscriptionPlan(SubscriptionPlan.FREE)
                .dailySearchCount(5) // was at limit yesterday
                .dailySearchResetAt(yesterday)
                .roles(Set.of())
                .build();
        user.setId(userId);

        RecordSearchRequest req = new RecordSearchRequest();
        req.setQueryType(QueryType.IMAGE);
        req.setQueryValue("image-data");
        req.setResultCount(1);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(searchHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.recordSearch(userId, req); // Must not throw — counter was reset

        assertThat(user.getDailySearchCount()).isEqualTo(1); // reset to 0 then +1
    }

    @Test
    void recordSearch_freeUserFirstSearchEver_setsResetAt() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .subscriptionPlan(SubscriptionPlan.FREE)
                .dailySearchCount(0)
                .dailySearchResetAt(null) // never searched before
                .roles(Set.of())
                .build();
        user.setId(userId);

        RecordSearchRequest req = new RecordSearchRequest();
        req.setQueryType(QueryType.TEXT);
        req.setQueryValue("first search");

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(searchHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.recordSearch(userId, req);

        assertThat(user.getDailySearchResetAt()).isNotNull();
        assertThat(user.getDailySearchCount()).isEqualTo(1);
    }
}
