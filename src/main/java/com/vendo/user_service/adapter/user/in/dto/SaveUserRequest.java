package com.vendo.user_service.adapter.user.in.dto;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserRole;
import com.vendo.user_lib.type.UserStatus;
import lombok.Builder;

@Builder
public record SaveUserRequest(
        String email,
        UserRole role,
        UserStatus status,
        ProviderType providerType,
        String password,
        Boolean emailVerified) {
}

