package com.vendo.user_service.domain.user;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserStatus;

import java.time.LocalDate;

public class UserDataBuilder {

    public static User.UserBuilder withAllFields() {
        return User.builder()
                .email("test@mail.com")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .providerType(ProviderType.LOCAL)
                .password("password")
                .birthDate(LocalDate.now())
                .fullName("Full Name");
    }

}
