package com.pricehawk.catalog.controller;

import com.pricehawk.catalog.dto.request.CreateCategoryRequest;
import com.pricehawk.catalog.dto.response.CategoryDTO;
import com.pricehawk.catalog.service.CategoryService;
import com.pricehawk.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getRootCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getRootCategories()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getBySlug(slug)));
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getChildren(@PathVariable UUID parentId) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getChildren(parentId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDTO>> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(categoryService.create(request)));
    }
}
