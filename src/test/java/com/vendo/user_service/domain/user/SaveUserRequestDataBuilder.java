package com.vendo.user_service.domain.user;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserRole;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.domain.user.dto.SaveUserRequest;

public class SaveUserRequestDataBuilder {

    public static SaveUserRequest.SaveUserRequestBuilder withAllFields() {

        return SaveUserRequest.builder()
                .email("test@mail.com")
                .emailVerified(true)
                .password("password")
                .providerType(ProviderType.LOCAL)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE);

    }

}
