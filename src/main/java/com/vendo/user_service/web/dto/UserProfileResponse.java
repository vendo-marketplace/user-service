package com.vendo.user_service.web.dto;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.type.UserRole;

import java.time.LocalDate;

public record UserProfileResponse(
        String id,
        String email,
        UserRole role,
        UserStatus status,
        ProviderType providerType,
        LocalDate birthDate,
        String fullName
) {
}
