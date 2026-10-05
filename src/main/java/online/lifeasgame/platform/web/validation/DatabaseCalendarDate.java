package online.lifeasgame.platform.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DatabaseCalendarDateValidator.class)
public @interface DatabaseCalendarDate {
    String message() default "Date must be between 1000-01-01 and 9999-12-31";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
