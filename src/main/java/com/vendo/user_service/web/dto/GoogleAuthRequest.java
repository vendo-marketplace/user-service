package com.vendo.user_service.web.dto;

import jakarta.validation.constraints.NotNull;

public record GoogleAuthRequest(@NotNull(message = "Id Token is required") String idToken) {
}
