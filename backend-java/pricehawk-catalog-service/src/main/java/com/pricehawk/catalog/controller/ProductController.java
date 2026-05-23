package com.pricehawk.catalog.controller;

import com.pricehawk.catalog.dto.request.CreateProductRequest;
import com.pricehawk.catalog.dto.request.UpdateProductRequest;
import com.pricehawk.catalog.dto.response.PriceHistoryDTO;
import com.pricehawk.catalog.dto.response.ProductDTO;
import com.pricehawk.catalog.dto.response.ProductSummaryDTO;
import com.pricehawk.catalog.service.ProductService;
import com.pricehawk.common.response.ApiResponse;
import com.pricehawk.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryDTO>>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minSentimentScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

        return ResponseEntity.ok(ApiResponse.ok(
            productService.list(categoryId, brand, minPrice, maxPrice, minSentimentScore,
                PageRequest.of(page, size, sort))
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductDTO>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getBySlug(slug)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(productService.create(request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productService.update(id, request)));
    }

    @GetMapping("/listings/{listingId}/price-history")
    public ResponseEntity<ApiResponse<List<PriceHistoryDTO>>> getPriceHistory(
            @PathVariable UUID listingId,
            @RequestParam(defaultValue = "90") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getPriceHistory(listingId, limit)));
    }
}
