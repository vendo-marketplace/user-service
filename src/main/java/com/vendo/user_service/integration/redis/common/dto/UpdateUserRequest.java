package com.vendo.user_service.integration.redis.common.dto;

import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.common.type.UserStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UpdateUserRequest(
        String email,
        UserRole role,
        UserStatus status,
        String password,
        LocalDate birthDate,
        String fullName
) {
}
