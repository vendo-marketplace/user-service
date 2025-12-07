package com.vendo.user_service.web.dto;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record UserUpdateRequest(
        Boolean emailVerified,
        UserStatus status,
        ProviderType providerType,
        String password,
        LocalDate birthDate,
        String fullName
) {
}
