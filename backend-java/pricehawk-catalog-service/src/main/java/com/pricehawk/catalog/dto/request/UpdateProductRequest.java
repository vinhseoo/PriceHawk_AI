package com.pricehawk.catalog.dto.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * All fields nullable — only non-null fields are applied (PATCH semantics).
 */
public record UpdateProductRequest(
    @Size(max = 500) String name,
    @Size(max = 255) String brand,
    String description,
    @Size(max = 500) String thumbnailUrl,
    UUID categoryId
) {}
