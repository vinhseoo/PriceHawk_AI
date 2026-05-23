package com.pricehawk.catalog.service;

import com.pricehawk.catalog.domain.entity.Category;
import com.pricehawk.catalog.dto.request.CreateCategoryRequest;
import com.pricehawk.catalog.dto.response.CategoryDTO;
import com.pricehawk.catalog.mapper.CatalogMapper;
import com.pricehawk.catalog.repository.CategoryRepository;
import com.pricehawk.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getRootCategories() {
        return categoryRepository.findRootCategories()
            .stream()
            .map(catalogMapper::toCategoryDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDTO getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
            .orElseThrow(() -> BusinessException.notFound("Category"));
        return catalogMapper.toCategoryDTO(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getChildren(UUID parentId) {
        // Validate parent exists
        if (!categoryRepository.existsById(parentId)) {
            throw BusinessException.notFound("Category");
        }
        return categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrder(parentId)
            .stream()
            .map(catalogMapper::toCategoryDTOFlat)
            .toList();
    }

    @Transactional
    public CategoryDTO create(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw BusinessException.conflict("Category slug already exists");
        }

        Category.CategoryBuilder builder = Category.builder()
            .name(request.name())
            .slug(request.slug())
            .sortOrder(request.sortOrder());

        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                .orElseThrow(() -> BusinessException.notFound("Parent category"));
            builder.parent(parent).level(parent.getLevel() + 1);
        }

        Category saved = categoryRepository.save(builder.build());
        log.info("Category created: id={}, slug={}", saved.getId(), saved.getSlug());
        return catalogMapper.toCategoryDTO(saved);
    }
}
