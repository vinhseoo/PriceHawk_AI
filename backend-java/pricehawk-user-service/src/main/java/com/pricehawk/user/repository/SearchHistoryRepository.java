package com.pricehawk.user.repository;

import com.pricehawk.user.domain.entity.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    Page<SearchHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
