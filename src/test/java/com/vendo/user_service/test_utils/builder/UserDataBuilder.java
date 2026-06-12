package com.vendo.user_service.test_utils.builder;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserStatus;
import com.vendo.user_service.domain.user.User;

import java.time.LocalDate;

public class UserDataBuilder {

    public static User.UserBuilder withAllFields() {
        return User.builder()
                .id("id")
                .email("test@mail.com")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .providerType(ProviderType.LOCAL)
                .password("password")
                .birthDate(LocalDate.now())
                .fullName("Full Name");
    }

}
