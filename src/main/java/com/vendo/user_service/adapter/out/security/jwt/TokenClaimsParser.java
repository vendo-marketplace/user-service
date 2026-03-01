package com.vendo.user_service.adapter.out.security.jwt;

import com.vendo.user_service.adapter.out.security.jwt.dto.InternalClaimPayload;

public interface TokenClaimsParser {

    InternalClaimPayload parseInternalClaims(String token);

}
