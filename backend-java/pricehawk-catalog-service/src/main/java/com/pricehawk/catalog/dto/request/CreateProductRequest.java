package com.pricehawk.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProductRequest(
    @NotBlank @Size(max = 500) String name,
    @NotBlank @Size(max = 500) String slug,
    @Size(max = 255) String brand,
    String description,
    @Size(max = 500) String thumbnailUrl,
    UUID categoryId
) {}
