package com.vendo.user_service.domain.user;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.domain.user.dto.UpdateUserRequest;

import java.time.LocalDate;

public class UpdateUserRequestDataBuilder {

    public static UpdateUserRequest.UpdateUserRequestBuilder withAllFields() {

        return UpdateUserRequest.builder()
                .fullName("Full Name")
                .password("password")
                .providerType(ProviderType.LOCAL)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .birthDate(LocalDate.now());

    }

}
