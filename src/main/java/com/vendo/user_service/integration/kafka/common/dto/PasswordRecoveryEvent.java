package com.vendo.user_service.integration.kafka.common.dto;

import lombok.Builder;

@Builder
public record PasswordRecoveryEvent(String email, String token) {
}
