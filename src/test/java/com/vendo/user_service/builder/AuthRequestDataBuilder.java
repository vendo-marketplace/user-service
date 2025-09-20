package com.vendo.user_service.builder;

import com.vendo.user_service.web.dto.AuthRequest;

public class AuthRequestDataBuilder {

    public static AuthRequest.AuthRequestBuilder buildUserWithRequiredFields() {
        return AuthRequest.builder()
                .email("test@gmail.com")
                .password("Qwerty1234@");
    }

}
