package com.vendo.user_service.adapter.in.user.dto;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserStatus;
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
