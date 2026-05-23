package com.pricehawk.user.dto.request;

import com.pricehawk.user.domain.entity.QueryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecordSearchRequest {

    @NotNull
    private QueryType queryType;

    @NotBlank
    @Size(max = 2000)
    private String queryValue;

    private Integer resultCount;
}
