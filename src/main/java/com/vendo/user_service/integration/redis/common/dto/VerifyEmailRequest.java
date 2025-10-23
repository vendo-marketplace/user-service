package com.vendo.user_service.integration.redis.common.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record VerifyEmailRequest(
        @NotNull(message = "Otp is required")
        @Digits(integer = 6, fraction = 0, message = "Otp should have 6 digits")
        Integer otp,

        @NotBlank(message = "Email is required")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email")
        String email
) {
}
