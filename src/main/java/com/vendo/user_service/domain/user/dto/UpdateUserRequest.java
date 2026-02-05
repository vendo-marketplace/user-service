package com.vendo.user_service.domain.user.dto;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UpdateUserRequest(
        String fullName,
        LocalDate birthDate,
        Boolean emailVerified,
        String password,
        UserStatus status,
        ProviderType providerType) {
}
