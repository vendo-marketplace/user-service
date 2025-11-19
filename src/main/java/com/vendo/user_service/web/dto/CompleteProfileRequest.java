package com.vendo.user_service.web.dto;

import com.vendo.user_service.common.annotation.Adult;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CompleteProfileRequest(

        @Pattern(regexp = "^[A-ZА-ЯІЇЄҐ][a-zа-яіїєґ]+ [A-ZА-ЯІЇЄҐ][a-zа-яіїєґ]+$", message = "Full name must contain two words, each starting with an uppercase letter and followed by lowercase letters.")
        String fullName,

        @Adult(message = "User should be at least 18 years old.")
        LocalDate birthDate) {
}
