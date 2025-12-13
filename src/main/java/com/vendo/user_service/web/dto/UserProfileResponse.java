package com.vendo.user_service.web.dto;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.security.common.type.UserAuthority;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record UserProfileResponse(
        String id,
        String email,
        UserAuthority role,
        UserStatus status,
        ProviderType providerType,
        LocalDate birthDate,
        String fullName,
        Instant createdAt,
        Instant updatedAt) {
}
