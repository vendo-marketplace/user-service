package com.vendo.user_service.common.builder;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.model.User;
import com.vendo.user_service.web.dto.UserProfileResponse;

import java.time.Instant;
import java.time.LocalDate;

public class UserDataBuilder {

    public static User.UserBuilder buildUserWithRequiredFields() {
        return User.builder()
                .id("1")
                .email("test@gmail.com")
                .password("Qwerty1234@")
                .role(UserRole.USER)
                .providerType(ProviderType.LOCAL)
                .status(UserStatus.INCOMPLETE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .fullName("John Doe")
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
    }

    public static UserProfileResponse buildUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getProviderType(),
                user.getBirthDate(),
                user.getFullName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
