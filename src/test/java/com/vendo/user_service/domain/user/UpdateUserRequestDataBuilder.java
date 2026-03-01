package com.vendo.user_service.domain.user;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserStatus;
import com.vendo.user_service.adapter.in.user.dto.UpdateUserRequest;

import java.time.LocalDate;

public class UpdateUserRequestDataBuilder {

    public static UpdateUserRequest.UpdateUserRequestBuilder withAllFields() {
        return UpdateUserRequest.builder()
                .fullName("full_name")
                .birthDate(LocalDate.now())
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .providerType(ProviderType.LOCAL);
    }

}
