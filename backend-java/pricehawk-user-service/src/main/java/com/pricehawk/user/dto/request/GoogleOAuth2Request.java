package com.pricehawk.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleOAuth2Request {

    @NotBlank
    private String idToken;
}
