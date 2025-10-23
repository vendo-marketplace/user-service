package com.vendo.user_service.integration.redis.common.dto;

import com.vendo.domain.user.common.type.UserStatus;
import lombok.Builder;

@Builder
public record UpdateUserRequest(
        UserStatus status,
        String password) {
}
