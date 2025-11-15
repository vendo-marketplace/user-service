package com.vendo.user_service.common.builder;

import com.vendo.user_service.security.common.dto.TokenPayload;

public class TokenPayloadDataBuilder {

    public static TokenPayload.TokenPayloadBuilder buildTokenPayloadWithAllFields() {
        return TokenPayload.builder()
                .accessToken("test_access_token")
                .refreshToken("test_refresh_token");
    }
}
