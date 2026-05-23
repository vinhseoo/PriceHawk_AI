package com.pricehawk.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UrlSearchRequest(
    @NotBlank @URL @Size(max = 2000) String url
) {}
