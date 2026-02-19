package com.vendo.user_service.adapter.out.security.jwt.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record InternalClaimPayload(
        String subject,
        List<String> roles,
        String audience) {
}

