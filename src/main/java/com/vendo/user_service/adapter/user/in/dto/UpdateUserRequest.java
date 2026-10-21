package com.vendo.user_service.adapter.user.in.dto;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserStatus;
import lombok.Builder;

@Builder
public record UpdateUserRequest(
        String fullName,
        Boolean emailVerified,
        String password,
        UserStatus status,
        ProviderType providerType) {
}
