package com.pricehawk.catalog.service;

import com.pricehawk.catalog.domain.entity.Category;
import com.pricehawk.catalog.dto.request.CreateCategoryRequest;
import com.pricehawk.catalog.dto.response.CategoryDTO;
import com.pricehawk.catalog.mapper.CatalogMapper;
import com.pricehawk.catalog.repository.CategoryRepository;
import com.pricehawk.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock CatalogMapper catalogMapper;
    @InjectMocks CategoryService categoryService;

    @Test
    void getRootCategories_returnsAllRoots() {
        Category cat = Category.builder().name("Electronics").slug("electronics").build();
        CategoryDTO dto = new CategoryDTO(UUID.randomUUID(), "Electronics", "electronics",
            null, 0, 0, true, List.of());

        when(categoryRepository.findRootCategories()).thenReturn(List.of(cat));
        when(catalogMapper.toCategoryDTO(cat)).thenReturn(dto);

        List<CategoryDTO> result = categoryService.getRootCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Electronics");
    }

    @Test
    void getBySlug_found_returnsDTO() {
        Category cat = Category.builder().name("Laptop").slug("laptop").build();
        CategoryDTO dto = new CategoryDTO(UUID.randomUUID(), "Laptop", "laptop",
            null, 0, 1, true, List.of());

        when(categoryRepository.findBySlug("laptop")).thenReturn(Optional.of(cat));
        when(catalogMapper.toCategoryDTO(cat)).thenReturn(dto);

        CategoryDTO result = categoryService.getBySlug("laptop");

        assertThat(result.slug()).isEqualTo("laptop");
    }

    @Test
    void getBySlug_notFound_throwsNotFound() {
        when(categoryRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getBySlug("unknown"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Category");
    }

    @Test
    void create_duplicateSlug_throwsConflict() {
        when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

        var request = new CreateCategoryRequest("Electronics", "electronics", null, 1);

        assertThatThrownBy(() -> categoryService.create(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("slug");
    }

    @Test
    void create_withParent_setsLevelCorrectly() {
        UUID parentId = UUID.randomUUID();
        Category parent = Category.builder().name("Electronics").slug("electronics").level(0).build();
        parent.setId(parentId);

        Category saved = Category.builder().name("Laptop").slug("laptop").level(1).parent(parent).build();
        saved.setId(UUID.randomUUID());

        CategoryDTO dto = new CategoryDTO(saved.getId(), "Laptop", "laptop",
            parentId, 1, 0, true, List.of());

        when(categoryRepository.existsBySlug("laptop")).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any())).thenReturn(saved);
        when(catalogMapper.toCategoryDTO(saved)).thenReturn(dto);

        CategoryDTO result = categoryService.create(
            new CreateCategoryRequest("Laptop", "laptop", parentId, 0));

        assertThat(result.level()).isEqualTo(1);
        assertThat(result.parentId()).isEqualTo(parentId);
    }

    @Test
    void create_parentNotFound_throwsNotFound() {
        UUID parentId = UUID.randomUUID();
        when(categoryRepository.existsBySlug("laptop")).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(
            new CreateCategoryRequest("Laptop", "laptop", parentId, 0)))
            .isInstanceOf(BusinessException.class);
    }
}
