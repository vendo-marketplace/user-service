package com.vendo.user_service.model;

import com.vendo.user_service.builder.UserDataBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

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

        validateUserField(user, "Email is required");
    }

    @Test
    void whenEmailHasIncorrectFormat_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().email("testgmail.com").build();

        validateUserField(user, "Invalid email");
    }

    @Test
    void whenStatusIsNotPresent_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().status(null).build();

        validateUserField(user, "Status is required");
    }

    @Test
    void whenRoleIsNotPresent_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().role(null).build();

        validateUserField(user, "Role is required");
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

        validateUserField(user, "Password is required");
    }

    @Test
    void whenPasswordHasIncorrectFormat_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().password("qwerty1234").build();

        validateUserField(user, "Invalid password. Should include minimum 8 characters, 1 uppercase character, 1 lowercase character, 1 special symbol");
    }

    @Test
    void whenBirthDateIsNull_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().birthDate(null).build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenBirthDateIsInPast_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().birthDate(LocalDate.of(2000, 2, 20)).build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenBirthDateIsToday_thenViolationsFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().birthDate(LocalDate.now()).build();

        validateUserField(user, "Birth date must be in the past");
    }

    @Test
    void whenBirthDateIsInFuture_thenValidationFalls() {
        User user = UserDataBuilder.buildUserWithRequiredFields().birthDate(LocalDate.now().plusDays(1)).build();

        validateUserField(user, "Birth date must be in the past");
    }

    @Test
    void whenFullNameIsNull_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName(null).build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenFullNameIsTwoWordsValid_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John Smith").build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenFullNameIsThreeWordsValid_thenNoViolations() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John Smith Junior").build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenFullNameStartsWithLowercase_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("john Smith").build();

        validateUserField(user, "Invalid full name. Should contain 2-3 words, each starting with capital letter");
    }

    @Test
    void whenFullNameHasOnlyOneWord_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John").build();

        validateUserField(user, "Invalid full name. Should contain 2-3 words, each starting with capital letter");
    }

    @Test
    void whenFullNameHasFourWords_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John Smith Junior Senior").build();

        validateUserField(user, "Invalid full name. Should contain 2-3 words, each starting with capital letter");
    }

    @Test
    void whenFullNameContainsDigits_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John Smith2").build();

        validateUserField(user, "Invalid full name. Should contain 2-3 words, each starting with capital letter");
    }

    @Test
    void whenFullNameContainsDoubleSpaces_thenValidationFails() {
        User user = UserDataBuilder.buildUserWithRequiredFields().fullName("John  Smith").build();

        validateUserField(user, "Invalid full name. Should contain 2-3 words, each starting with capital letter");
    }

    private void validateUserField(User user, String validationMessage) {
        Set<ConstraintViolation<User>> constraintViolations = validator.validate(user);

        assertThat(constraintViolations.size()).isEqualTo(1);
        assertThat(constraintViolations.stream()
                .anyMatch(cv -> cv.getMessage().equals(validationMessage)))
                .isTrue();
    }
}