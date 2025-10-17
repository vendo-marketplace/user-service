package com.vendo.user_service.integration.redis.common.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record ResetPasswordRequest(
        @NotNull(message = "Otp is required")
        @Digits(integer = 6, fraction = 0, message = "Otp should have 6 digits")
        Integer otp,
        @NotNull(message = "Password is required")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$", message = "Invalid password. Should include minimum 8 characters, 1 uppercase character, 1 lowercase character, 1 special symbol")
        String password) {
}
