package com.vendo.user_service.integration.redis.common.dto;

import lombok.Builder;

@Builder
public record UpdateUserRequest(String password) {
}
