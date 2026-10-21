package com.vendo.user_service.test_utils.builder;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserStatus;
import com.vendo.user_service.adapter.user.in.dto.UpdateUserRequest;

public class UpdateUserRequestDataBuilder {

    public static UpdateUserRequest.UpdateUserRequestBuilder withAllFields() {
        return UpdateUserRequest.builder()
                .fullName("full_name")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .providerType(ProviderType.LOCAL);
    }

}
