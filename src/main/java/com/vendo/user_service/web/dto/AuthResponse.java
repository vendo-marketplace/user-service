package com.vendo.user_service.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AuthResponse (
        @JsonProperty("access-token")
        String accessToken,

        @JsonProperty("refresh-token")
        String refreshToken) {
}
