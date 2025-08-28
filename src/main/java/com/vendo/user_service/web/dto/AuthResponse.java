package com.vendo.user_service.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    @JsonProperty("access-token")
    private String accessToken;

    @JsonProperty("refresh-token")
    private String refreshToken;

}
