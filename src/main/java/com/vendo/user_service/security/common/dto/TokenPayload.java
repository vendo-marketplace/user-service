package com.vendo.user_service.security.common.dto;

import lombok.Builder;

@Builder
public record TokenPayload(
        String accessToken,
        String refreshToken) {
}
