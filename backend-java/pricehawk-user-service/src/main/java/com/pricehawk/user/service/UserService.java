package com.pricehawk.user.service;

import com.pricehawk.common.exception.BusinessException;
import com.pricehawk.common.response.PageResponse;
import com.pricehawk.user.domain.entity.SearchHistory;
import com.pricehawk.user.domain.entity.SubscriptionPlan;
import com.pricehawk.user.domain.entity.User;
import com.pricehawk.user.dto.request.RecordSearchRequest;
import com.pricehawk.user.dto.request.UpdateProfileRequest;
import com.pricehawk.user.dto.response.SearchHistoryDTO;
import com.pricehawk.user.dto.response.UserDTO;
import com.pricehawk.user.mapper.UserMapper;
import com.pricehawk.user.repository.SearchHistoryRepository;
import com.pricehawk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int FREE_DAILY_LIMIT = 5;

    private final UserRepository userRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserDTO getProfile(UUID userId) {
        // findByIdWithRoles: single JOIN FETCH query — không trigger lazy load sau
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> BusinessException.notFound("User"));
        return userMapper.toDTO(user);
    }

    @Transactional
    public UserDTO updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> BusinessException.notFound("User"));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getPreferences() != null) user.setPreferences(request.getPreferences());

        log.info("Profile updated: userId={}", userId);
        return userMapper.toDTO(userRepository.save(user));
    }

    /**
     * Records a search and enforces the daily limit for FREE users.
     *
     * <p>Uses PESSIMISTIC_WRITE lock on the user row to prevent race conditions —
     * without the lock, two concurrent requests could both pass the limit check
     * and both increment the counter, effectively bypassing the cap.
     */
    @Transactional
    public void recordSearch(UUID userId, RecordSearchRequest request) {
        // Acquires a row-level write lock for the duration of this transaction.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> BusinessException.notFound("User"));

        resetDailyCountIfNeeded(user);

        if (user.getSubscriptionPlan() == SubscriptionPlan.FREE
                && user.getDailySearchCount() >= FREE_DAILY_LIMIT) {
            log.warn("Rate limit hit: userId={}, count={}", userId, user.getDailySearchCount());
            throw BusinessException.forbidden(
                    "Daily search limit reached (" + FREE_DAILY_LIMIT + "/day). " +
                    "Upgrade to PREMIUM for unlimited searches.");
        }

        user.setDailySearchCount(user.getDailySearchCount() + 1);
        userRepository.save(user);

        searchHistoryRepository.save(SearchHistory.builder()
                .userId(userId)
                .queryType(request.getQueryType())
                .queryValue(request.getQueryValue())
                .resultCount(request.getResultCount())
                .build());

        log.debug("Search recorded: userId={}, type={}, count={}",
                userId, request.getQueryType(), user.getDailySearchCount());
    }

    @Transactional(readOnly = true)
    public PageResponse<SearchHistoryDTO> getSearchHistory(UUID userId, int page, int size) {
        Page<SearchHistoryDTO> result = searchHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(userMapper::toDTO);

        return PageResponse.<SearchHistoryDTO>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    private void resetDailyCountIfNeeded(User user) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        boolean needsReset = user.getDailySearchResetAt() == null
                || user.getDailySearchResetAt()
                       .atZone(ZoneOffset.UTC).toLocalDate()
                       .isBefore(today);

        if (needsReset) {
            log.debug("Daily count reset: userId={}, previousCount={}",
                    user.getId(), user.getDailySearchCount());
            user.setDailySearchCount(0);
            user.setDailySearchResetAt(today.atStartOfDay(ZoneOffset.UTC).toInstant());
        }
    }
}
