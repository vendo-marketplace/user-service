package com.vendo.user_service.adapter.in.user.dto;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserRole;
import com.vendo.domain.user.common.type.UserStatus;
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

