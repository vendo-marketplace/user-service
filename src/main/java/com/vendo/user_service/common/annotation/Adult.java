package com.vendo.user_service.common.annotation;

import com.vendo.user_service.common.annotation.validator.AdultValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Constraint(validatedBy = AdultValidator.class)
public @interface Adult {

    String message() default "";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
