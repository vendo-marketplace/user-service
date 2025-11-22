package com.vendo.user_service.model;

import com.vendo.user_service.common.builder.UserDataBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserValidationTest {

    Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void whenUserIsValid_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        Set<ConstraintViolation<User>> constraintViolations = validator.validate(user);
        assertThat(constraintViolations.isEmpty()).isTrue();
    }

    @Test
    void whenEmailIsValid_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().email("test@gmail.com").build();

        Set<ConstraintViolation<User>> constraintViolations = validator.validate(user);

        assertThat(constraintViolations.size()).isEqualTo(0);
    }

    @Test
    void whenEmailIsNotPresent_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().email(null).build();

        validateUserField(user, "Email is required.");
    }

    @Test
    void whenEmailHasIncorrectFormat_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().email("testgmail.com").build();

        validateUserField(user, "Invalid email.");
    }

    @Test
    void whenStatusIsNotPresent_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().status(null).build();

        validateUserField(user, "Status is required.");
    }

    @Test
    void whenRoleIsNotPresent_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().role(null).build();

        validateUserField(user, "Role is required.");
    }

    @Test
    void whenPasswordIsValid_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().password("Qwerty1234@").build();

        Set<ConstraintViolation<User>> constraintViolations = validator.validate(user);

        assertThat(constraintViolations.size()).isEqualTo(0);
    }

    @Test
    void whenPasswordIsNotPresent_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().password(null).build();

        validateUserField(user, "Password is required.");
    }

    @Test
    void whenPasswordHasIncorrectFormat_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().password("qwerty1234").build();

        validateUserField(user, "Invalid password. Should include minimum 8 characters, 1 uppercase character, 1 lowercase character, 1 special symbol.");
    }

    @Test
    void whenBirthDateIsNull_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().birthDate(null).build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenAdult_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().birthDate(LocalDate.of(2000, 1, 1)).build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenNotAdult_thenViolationsFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().birthDate(LocalDate.now()).build();

        validateUserField(user, "User should be at least 18 years old.");
    }

    @Test
    void whenFullNameIsNull_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName(null).build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenFullNameIsTwoWords_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John Smith").build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenFullNameIsThreeWords_ValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John Smith Junior").build();

        validateUserField(user, "Full name must contain two words, each starting with an uppercase letter and followed by lowercase letters.");

    }

    @Test
    void whenFullNameStartsWithLowercase_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("john Smith").build();

        validateUserField(user, "Full name must contain two words, each starting with an uppercase letter and followed by lowercase letters.");
    }

    @Test
    void whenFullNameHasOnlyOneWord_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John").build();

        validateUserField(user, "Full name must contain two words, each starting with an uppercase letter and followed by lowercase letters.");
    }

    @Test
    void whenFullNameContainsDigits_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John Smith2").build();

        validateUserField(user, "Full name must contain two words, each starting with an uppercase letter and followed by lowercase letters.");
    }

    @Test
    void whenFullNameContainsDoubleSpaces_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John  Smith").build();

        validateUserField(user, "Full name must contain two words, each starting with an uppercase letter and followed by lowercase letters.");
    }

    private void validateUserField(User user, String validationMessage) {
        Set<ConstraintViolation<User>> constraintViolations = validator.validate(user);

        assertThat(constraintViolations.size()).isEqualTo(1);
        assertThat(constraintViolations.stream()
                .anyMatch(cv -> cv.getMessage().equals(validationMessage)))
                .isTrue();
    }
}