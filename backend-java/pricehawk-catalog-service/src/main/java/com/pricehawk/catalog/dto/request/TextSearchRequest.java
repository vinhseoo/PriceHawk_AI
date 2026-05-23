package com.pricehawk.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextSearchRequest(
    @NotBlank @Size(max = 500) String query,
    int page,
    int size
) {
    public TextSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
    }
}
