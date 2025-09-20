package com.vendo.user_service.security.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenPayload {

    private String accessToken;

    private String refreshToken;

}
