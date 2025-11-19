package com.vendo.user_service.common.annotation.validator;

import com.vendo.user_service.common.annotation.Adult;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {

    private static final int EIGHTEEN_YEARS = 18;

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        return !value.plusYears(EIGHTEEN_YEARS).isAfter(LocalDate.now());
    }
}
