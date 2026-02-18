package com.vendo.user_service.adapter.out.security.jwt.dto;

import java.util.List;

public record InternalClaimPayload(
        String subject,
        List<String> roles,
        String audience) {
}

