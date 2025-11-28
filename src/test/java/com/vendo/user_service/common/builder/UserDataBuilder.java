package com.vendo.user_service.common.builder;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.model.User;

import java.time.Instant;
import java.time.LocalDate;

public class UserDataBuilder {

    public static User.UserBuilder buildUserAllFields() {
        return User.builder()
                .id("1")
                .email("test@gmail.com")
                .password("Qwerty1234@")
                .role(UserRole.USER)
                .fullName("Test Name")
                .birthDate(LocalDate.of(2000, 1, 1))
                .providerType(ProviderType.LOCAL)
                .status(UserStatus.INCOMPLETE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
    }
}
