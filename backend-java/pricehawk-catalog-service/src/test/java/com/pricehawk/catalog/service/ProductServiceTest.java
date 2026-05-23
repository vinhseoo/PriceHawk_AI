package com.pricehawk.catalog.service;

import com.pricehawk.catalog.domain.entity.Product;
import com.pricehawk.catalog.dto.request.CreateProductRequest;
import com.pricehawk.catalog.dto.request.UpdateProductRequest;
import com.pricehawk.catalog.dto.response.ProductDTO;
import com.pricehawk.catalog.mapper.CatalogMapper;
import com.pricehawk.catalog.repository.CategoryRepository;
import com.pricehawk.catalog.repository.PriceHistoryRepository;
import com.pricehawk.catalog.repository.ProductRepository;
import com.pricehawk.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock PriceHistoryRepository priceHistoryRepository;
    @Mock CatalogMapper catalogMapper;
    @InjectMocks ProductService productService;

    @Test
    void getById_found_returnsDTO() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder().name("iPhone 14").slug("iphone-14").build();
        product.setId(id);

        ProductDTO dto = buildProductDTO(id, "iPhone 14", "iphone-14");

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(catalogMapper.toProductDTO(product)).thenReturn(dto);

        ProductDTO result = productService.getById(id);

        assertThat(result.name()).isEqualTo("iPhone 14");
    }

    @Test
    void getById_notFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Product");
    }

    @Test
    void getBySlug_notFound_throwsNotFound() {
        when(productRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getBySlug("unknown"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void create_duplicateSlug_throwsConflict() {
        when(productRepository.existsBySlug("iphone-14")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(
            new CreateProductRequest("iPhone 14", "iphone-14", "Apple", null, null, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("slug");
    }

    @Test
    void create_newProduct_savesAndReturnsDTO() {
        UUID id = UUID.randomUUID();
        Product saved = Product.builder().name("iPhone 14").slug("iphone-14").build();
        saved.setId(id);

        ProductDTO dto = buildProductDTO(id, "iPhone 14", "iphone-14");

        when(productRepository.existsBySlug("iphone-14")).thenReturn(false);
        when(productRepository.save(any())).thenReturn(saved);
        when(catalogMapper.toProductDTO(saved)).thenReturn(dto);

        ProductDTO result = productService.create(
            new CreateProductRequest("iPhone 14", "iphone-14", "Apple", null, null, null));

        assertThat(result.slug()).isEqualTo("iphone-14");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_patchesOnlyNonNullFields() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder().name("iPhone 14").slug("iphone-14").brand("Apple").build();
        product.setId(id);

        Product saved = Product.builder().name("iPhone 14 Pro").slug("iphone-14").brand("Apple").build();
        saved.setId(id);
        ProductDTO dto = buildProductDTO(id, "iPhone 14 Pro", "iphone-14");

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(saved);
        when(catalogMapper.toProductDTO(saved)).thenReturn(dto);

        // Only name is patched; brand and categoryId are null → should not be changed
        ProductDTO result = productService.update(id, new UpdateProductRequest("iPhone 14 Pro", null, null, null, null));

        assertThat(result.name()).isEqualTo("iPhone 14 Pro");
        // brand was not in the patch, verify original brand was kept
        verify(productRepository).save(argThat(p -> "Apple".equals(p.getBrand())));
    }

    @Test
    void update_notFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(id,
            new UpdateProductRequest("new name", null, null, null, null)))
            .isInstanceOf(BusinessException.class);
    }

    private ProductDTO buildProductDTO(UUID id, String name, String slug) {
        return new ProductDTO(id, name, slug, "Apple", null, null,
            null, null, null, 0, null, null, null, null, null, null, null);
    }
}
