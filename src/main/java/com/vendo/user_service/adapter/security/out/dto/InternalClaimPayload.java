package com.vendo.user_service.adapter.security.out.dto;

import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder
public record InternalClaimPayload(
        String subject,
        List<String> roles,
        Set<String> audience) {
}

