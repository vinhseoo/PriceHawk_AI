package com.pricehawk.catalog.repository;

import com.pricehawk.catalog.domain.entity.Category;
import com.pricehawk.data.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends BaseRepository<Category> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Fetch top-level categories (Electronics, Fashion, etc.)
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.sortOrder")
    List<Category> findRootCategories();

    // Fetch all active children of a given parent
    List<Category> findByParentIdAndIsActiveTrueOrderBySortOrder(UUID parentId);

    List<Category> findByLevelAndIsActiveTrue(int level);
}
