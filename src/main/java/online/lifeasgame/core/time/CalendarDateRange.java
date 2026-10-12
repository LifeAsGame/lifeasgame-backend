package online.lifeasgame.core.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public final class CalendarDateRange {
    private static final LocalDate FIRST_DATABASE_DATE = LocalDate.of(1000, 1, 1);
    private static final LocalDate LAST_DATABASE_DATE = LocalDate.of(9999, 12, 31);
    // Matches hibernate.jdbc.time_zone in application.yml for DATETIME(6) columns.
    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Seoul");

    private CalendarDateRange() {}

    public static boolean inDateColumn(LocalDate date) {
        return date == null || !date.isBefore(FIRST_DATABASE_DATE) && !date.isAfter(LAST_DATABASE_DATE);
    }

    public static boolean inJson(LocalDate date) {
        return date == null || date.getYear() >= 1 && date.getYear() <= 9999;
    }

    public static boolean inDateTimeColumn(Instant instant) {
        if (instant == null) return true;
        try {
            return inDateColumn(instant.atZone(DATABASE_ZONE).toLocalDate());
        } catch (DateTimeException ignored) {
            return false;
        }
    }

    public static LocalDateTime parseDateTimeColumn(String value) {
        LocalDateTime dateTime = LocalDateTime.parse(value);
        if (!inDateColumn(dateTime.toLocalDate())) {
            throw new DateTimeParseException("Date is outside the supported range", value, 0);
        }
        return dateTime;
    }

    public static void requireDateColumn(LocalDate date) {
        if (!inDateColumn(date)) throw new IllegalArgumentException("Date is outside the supported range");
    }
}
