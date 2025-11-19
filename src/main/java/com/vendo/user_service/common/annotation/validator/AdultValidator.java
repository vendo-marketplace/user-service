package com.vendo.user_service.common.annotation.validator;

import com.vendo.user_service.common.annotation.Adult;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AdultValidator implements ConstraintValidator<Adult, String> {

    private static final int EIGHTEEN_YEARS = 18;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        try {
            LocalDate date = LocalDate.parse(value);
            return !date.plusYears(EIGHTEEN_YEARS).isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
