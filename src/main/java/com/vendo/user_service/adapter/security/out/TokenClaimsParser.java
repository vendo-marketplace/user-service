package com.vendo.user_service.adapter.security.out;

import com.vendo.user_service.adapter.security.out.dto.InternalClaimPayload;

public interface TokenClaimsParser {

    InternalClaimPayload parseInternalClaims(String token);

}
