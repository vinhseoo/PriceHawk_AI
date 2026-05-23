package com.pricehawk.catalog.service;

import com.pricehawk.catalog.domain.entity.Category;
import com.pricehawk.catalog.domain.entity.Product;
import com.pricehawk.catalog.dto.request.CreateProductRequest;
import com.pricehawk.catalog.dto.request.UpdateProductRequest;
import com.pricehawk.catalog.dto.response.PriceHistoryDTO;
import com.pricehawk.catalog.dto.response.ProductDTO;
import com.pricehawk.catalog.dto.response.ProductSummaryDTO;
import com.pricehawk.catalog.mapper.CatalogMapper;
import com.pricehawk.catalog.repository.CategoryRepository;
import com.pricehawk.catalog.repository.PriceHistoryRepository;
import com.pricehawk.catalog.repository.ProductRepository;
import com.pricehawk.catalog.repository.specification.ProductSpecification;
import com.pricehawk.common.exception.BusinessException;
import com.pricehawk.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CatalogMapper catalogMapper;

    @Transactional(readOnly = true)
    public ProductDTO getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
            .orElseThrow(() -> BusinessException.notFound("Product"));
        return catalogMapper.toProductDTO(product);
    }

    @Transactional(readOnly = true)
    public ProductDTO getById(UUID id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Product"));
        return catalogMapper.toProductDTO(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDTO> list(
            UUID categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minSentimentScore,
            Pageable pageable) {

        var filter = ProductSpecification.ProductFilter.builder()
            .categoryId(categoryId)
            .brand(brand)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .minSentimentScore(minSentimentScore)
            .build();

        Page<ProductSummaryDTO> page = productRepository
            .findAll(ProductSpecification.fromFilter(filter), pageable)
            .map(catalogMapper::toProductSummaryDTO);

        return PageResponse.<ProductSummaryDTO>builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();
    }

    @Transactional
    public ProductDTO create(CreateProductRequest request) {
        if (productRepository.existsBySlug(request.slug())) {
            throw BusinessException.conflict("Product slug already exists");
        }

        Product.ProductBuilder builder = Product.builder()
            .name(request.name())
            .slug(request.slug())
            .brand(request.brand())
            .description(request.description())
            .thumbnailUrl(request.thumbnailUrl());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> BusinessException.notFound("Category"));
            builder.category(category);
        }

        Product saved = productRepository.save(builder.build());
        log.info("Product created: id={}, slug={}", saved.getId(), saved.getSlug());
        return catalogMapper.toProductDTO(saved);
    }

    @Transactional
    public ProductDTO update(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Product"));

        if (request.name() != null)         product.setName(request.name());
        if (request.brand() != null)        product.setBrand(request.brand());
        if (request.description() != null)  product.setDescription(request.description());
        if (request.thumbnailUrl() != null) product.setThumbnailUrl(request.thumbnailUrl());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> BusinessException.notFound("Category"));
            product.setCategory(category);
        }

        return catalogMapper.toProductDTO(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryDTO> getPriceHistory(UUID listingId, int limit) {
        var pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return catalogMapper.toPriceHistoryDTOList(
            priceHistoryRepository.findByListingIdOrderByRecordedAtDesc(listingId, pageable)
        );
    }
}
