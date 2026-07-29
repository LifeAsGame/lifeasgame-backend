package online.lifeasgame.lifelog.domain.record;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.Objects;
import java.util.regex.Pattern;

public record LifeLogPeriodKey(String value) {

    private static final Pattern WEEKLY_PATTERN =
            Pattern.compile("\\d{4}-W\\d{2}");

    public LifeLogPeriodKey {
        Objects.requireNonNull(value, "periodKey must not be null");
        if (!WEEKLY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "periodKey must use YYYY-Www format"
            );
        }
        int week = Integer.parseInt(value.substring(6));
        int year = Integer.parseInt(value.substring(0, 4));
        int maximumWeek = LocalDate.of(year, 12, 28)
                .get(WeekFields.ISO.weekOfWeekBasedYear());
        if (week < 1 || week > maximumWeek) {
            throw new IllegalArgumentException(
                    "periodKey week is not valid for its ISO year"
            );
        }
    }

    public static LifeLogPeriodKey weekly(
            Instant occurredAt,
            ZoneId zoneId
    ) {
        ZonedDateTime local = Objects.requireNonNull(
                occurredAt,
                "occurredAt must not be null"
        ).atZone(Objects.requireNonNull(
                zoneId,
                "zoneId must not be null"
        ));
        WeekFields iso = WeekFields.ISO;
        int year = local.get(iso.weekBasedYear());
        int week = local.get(iso.weekOfWeekBasedYear());
        return new LifeLogPeriodKey(
                "%04d-W%02d".formatted(year, week)
        );
    }
}
