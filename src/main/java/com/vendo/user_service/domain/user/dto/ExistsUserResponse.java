package com.vendo.user_service.domain.user.dto;

import lombok.Builder;

@Builder
public record ExistsUserResponse(
        String status,
        boolean exists) {
}
