package com.vendo.user_service.model;

import com.vendo.user_service.common.builder.AuthRequestDataBuilder;
import com.vendo.user_service.common.builder.CompleteAuthRequestDataBuilder;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.CompleteAuthRequest;
import com.vendo.user_service.web.dto.GoogleAuthRequest;
import com.vendo.user_service.web.dto.RefreshRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoValidationTest {

    Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Nested
    class AuthRequestTests {

        @Test
        void authRequest_shouldPassValidation() {
            AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().build();

            Set<ConstraintViolation<AuthRequest>> constraintViolations = validator.validate(authRequest);
            assertThat(constraintViolations.isEmpty()).isTrue();
        }

        @Test
        void authRequest_whenEmailIsNotPresent_thenValidationFails() {
            AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().email(null).build();
            validateDtoField(authRequest, "Email is required.");
        }

        @Test
        void authRequest_whenEmailIsBlank_thenValidationFails() {
            AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().email("").build();
            validateDtoField(authRequest, "Invalid email.");
        }

        @Test
        void authRequest_whenEmailIsInvalid_thenValidationFails() {
            AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().email("invalid_email.com").build();
            validateDtoField(authRequest, "Invalid email.");
        }

        @Test
        void authRequest_whenPasswordIsNotPresent_thenValidationFails() {
            AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().password(null).build();
            validateDtoField(authRequest, "Password is required.");
        }

        @Test
        void authRequest_whenPasswordIsBlank_thenValidationFails() {
            AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().password("").build();
            validateDtoField(authRequest, "Invalid password. Should include minimum 8 characters, 1 uppercase character, 1 lowercase character, 1 special symbol.");
        }

        @Test
        void authRequest_whenPasswordIsInvalid_thenValidationFails() {
            AuthRequest authRequest = AuthRequestDataBuilder.buildUserWithAllFields().password("invalid_password").build();
            validateDtoField(authRequest, "Invalid password. Should include minimum 8 characters, 1 uppercase character, 1 lowercase character, 1 special symbol.");
        }
    }

    @Nested
    class CompleteAuthRequestTests {

        @Test
        void completeAuthRequest_shouldPassValidation() {
            CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields().build();

            Set<ConstraintViolation<CompleteAuthRequest>> constraintViolations = validator.validate(completeAuthRequest);
            assertThat(constraintViolations.isEmpty()).isTrue();
        }

        @Test
        void completeAuthRequest_whenFullNameIsNotPresent_thenValidationFails() {
            CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields()
                    .fullName(null)
                    .build();

            validateDtoField(completeAuthRequest, "Full name is required.");
        }

        @Test
        void completeAuthRequest_whenFullNameIsBlank_thenValidationFails() {
            CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields().fullName("").build();
            validateDtoField(completeAuthRequest, "Full name must contain two words, each starting with an uppercase letter and followed by lowercase letters.");
        }

        @Test
        void completeAuthRequest_whenBirthDateIsNotPresent_thenValidationFails() {
            CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields()
                    .birthDate(null)
                    .build();
            validateDtoField(completeAuthRequest, "Birth date is required.");
        }

        @Test
        void completeAuthRequest_whenBirthDateIsLessThanEighteenYears_thenValidationFails() {
            CompleteAuthRequest completeAuthRequest = CompleteAuthRequestDataBuilder.buildCompleteAuthRequestWithAllFields()
                    .birthDate(LocalDate.now().minusYears(17))
                    .build();
            validateDtoField(completeAuthRequest, "Birth date should be at least 18 years old.");
        }
    }

    @Nested
    class GoogleAuthRequestTests {

        @Test
        void googleAuthRequest_shouldPassValidation() {
            GoogleAuthRequest googleAuthRequest = GoogleAuthRequest.builder()
                    .idToken(String.valueOf(UUID.randomUUID()))
                    .build();

            Set<ConstraintViolation<GoogleAuthRequest>> constraintViolations = validator.validate(googleAuthRequest);
            assertThat(constraintViolations.isEmpty()).isTrue();
        }

        @Test
        void googleAuthRequest_whenIdTokenIsNotPresent_thenValidationFails() {
            GoogleAuthRequest googleAuthRequest = GoogleAuthRequest.builder().build();

            validateDtoField(googleAuthRequest, "Id Token is required.");
        }

        @Test
        void googleAuthRequest_whenIdTokenIsBlank_thenValidationFails() {
            GoogleAuthRequest googleAuthRequest = GoogleAuthRequest.builder()
                    .idToken("")
                    .build();
            validateDtoField(googleAuthRequest, "Id Token is required.");
        }
    }

    @Nested
    class RefreshRequestTests {

        @Test
        void refreshRequest_shouldPassValidation() {
            RefreshRequest refreshRequest = RefreshRequest.builder()
                    .refreshToken(String.valueOf(UUID.randomUUID()))
                    .build();

            Set<ConstraintViolation<RefreshRequest>> constraintViolations = validator.validate(refreshRequest);
            assertThat(constraintViolations.isEmpty()).isTrue();
        }

        @Test
        void refreshRequest_whenRefreshTokenIsNotPresent_thenValidationFails() {
            RefreshRequest refreshRequest = RefreshRequest.builder().build();
            validateDtoField(refreshRequest, "Refresh token is required.");
        }

        @Test
        void refreshRequest_whenRefreshTokenIsBlank_thenValidationFails() {
            RefreshRequest refreshRequest = RefreshRequest.builder()
                    .refreshToken(null)
                    .build();
            validateDtoField(refreshRequest, "Refresh token is required.");
        }
    }

    private <T> void validateDtoField(T dto, String... validationMessages) {
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(dto);

        assertThat(constraintViolations.size()).isEqualTo(validationMessages.length);
        assertThat(constraintViolations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet())
                .containsAll(List.of(validationMessages))
        ).isTrue();
    }
}