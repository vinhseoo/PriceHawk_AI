package com.pricehawk.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateProfileRequest {

    @Size(max = 255)
    private String fullName;

    @Size(max = 500)
    private String avatarUrl;

    private Map<String, Object> preferences;
}
