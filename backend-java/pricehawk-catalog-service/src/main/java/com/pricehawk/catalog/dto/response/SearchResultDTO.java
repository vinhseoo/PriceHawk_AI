package com.pricehawk.catalog.dto.response;

import java.util.List;

public record SearchResultDTO(
    List<ProductSummaryDTO> products,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {}
