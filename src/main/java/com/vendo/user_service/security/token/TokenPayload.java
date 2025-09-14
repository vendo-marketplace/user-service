package com.vendo.user_service.security.token;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenPayload {

    private String accessToken;

    private String refreshToken;

}
