package online.lifeasgame.platform.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import online.lifeasgame.core.time.CalendarDateRange;
import java.time.LocalDate;

public final class DatabaseCalendarDateValidator implements ConstraintValidator<DatabaseCalendarDate, LocalDate> {
    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        return CalendarDateRange.inDateColumn(value);
    }
}
