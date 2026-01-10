package com.vendo.user_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GoogleAuthRequest(@NotBlank(message = "Id Token is required.") String idToken) {
}
