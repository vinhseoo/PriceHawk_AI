package com.pricehawk.user.dto.response;

import com.pricehawk.user.domain.entity.QueryType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SearchHistoryDTO {

    private UUID id;
    private QueryType queryType;
    private String queryValue;
    private Integer resultCount;
    private Instant createdAt;
}
