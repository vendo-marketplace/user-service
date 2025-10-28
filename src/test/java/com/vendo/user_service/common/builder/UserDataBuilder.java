package com.vendo.user_service.common.builder;

import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.model.User;

public class UserDataBuilder {

    public static User.UserBuilder buildUserWithRequiredFields() {
        return User.builder()
                .email("test@gmail.com")
                .password("Qwerty1234@")
                .role(UserRole.USER)
                .status(UserStatus.INCOMPLETE);
    }

}
