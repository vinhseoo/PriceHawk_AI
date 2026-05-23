package com.pricehawk.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 255) String slug,
    UUID parentId,
    int sortOrder
) {}
