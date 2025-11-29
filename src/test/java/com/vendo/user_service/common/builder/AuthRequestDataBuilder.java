package com.vendo.user_service.common.builder;

import com.vendo.user_service.web.dto.AuthRequest;

public class AuthRequestDataBuilder {

    public static AuthRequest.AuthRequestBuilder buildUserWithAllFields() {
        return AuthRequest.builder()
                .email("test@gmail.com")
                .password("Qwerty1234@");
    }

}
