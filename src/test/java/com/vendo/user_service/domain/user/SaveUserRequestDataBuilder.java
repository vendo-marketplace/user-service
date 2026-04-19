package com.vendo.user_service.domain.user;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserRole;
import com.vendo.user_lib.type.UserStatus;
import com.vendo.user_service.adapter.user.in.dto.SaveUserRequest;

public class SaveUserRequestDataBuilder {

    public static SaveUserRequest.SaveUserRequestBuilder withAllFields() {
        return SaveUserRequest.builder()
                .role(UserRole.USER)
                .emailVerified(true)
                .providerType(ProviderType.LOCAL)
                .password("test_password")
                .status(UserStatus.ACTIVE)
                .email("test@gmail.com");
    }

}
