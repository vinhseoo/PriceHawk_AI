package com.pricehawk.catalog.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ScrapeJobDTO(
    UUID jobId,
    String url,
    String status,
    Instant requestedAt
) {}
