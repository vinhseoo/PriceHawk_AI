package com.pricehawk.catalog.dto.response;

import java.util.List;
import java.util.UUID;

public record CategoryDTO(
    UUID id,
    String name,
    String slug,
    UUID parentId,
    int level,
    int sortOrder,
    boolean isActive,
    List<CategoryDTO> children
) {}
