package com.vendo.user_service.adapter.user.in.dto;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserRole;
import com.vendo.user_lib.type.UserStatus;
import lombok.Builder;

import java.util.Set;

@Builder
public record SaveUserRequest(
        String email,
        String fullName,
        Set<UserRole> roles,
        UserStatus status,
        ProviderType providerType,
        String password,
        Boolean emailVerified) {
}

