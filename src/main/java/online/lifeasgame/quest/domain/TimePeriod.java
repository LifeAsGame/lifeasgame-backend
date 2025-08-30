package online.lifeasgame.quest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimePeriod {

    @Column(name = "period_start", nullable = false)
    private LocalDate start;

    @Column(name = "period_end", nullable = false)
    private LocalDate end;

    private TimePeriod(LocalDate s, LocalDate e) {
        Guard.notNull(s, "period.start");
        Guard.notNull(e, "period.end");
        Guard.check(!e.isBefore(s), "period end must be >= start");
        this.start = s;
        this.end = e;
    }

    public static TimePeriod of(LocalDate s, LocalDate e) { return new TimePeriod(s, e); }

    public static TimePeriod daily(LocalDate day){ return new TimePeriod(day, day); }

    public static TimePeriod weekly(LocalDate anyDay) {
        return weekly(anyDay, DayOfWeek.MONDAY);
    }

    public static TimePeriod weekly(LocalDate anyDay, DayOfWeek firstDayOfWeek) {
        Guard.notNull(anyDay, "anyDay");
        Guard.notNull(firstDayOfWeek, "firstDayOfWeek");
        LocalDate start = anyDay.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));
        LocalDate end   = start.plusDays(6);
        return new TimePeriod(start, end);
    }

    public static TimePeriod monthly(LocalDate anyDay) {
        Guard.notNull(anyDay, "anyDay");
        YearMonth ym = YearMonth.from(anyDay);
        return new TimePeriod(ym.atDay(1), ym.atEndOfMonth());
    }

    public static TimePeriod monthly(YearMonth ym) {
        Guard.notNull(ym, "yearMonth");
        return new TimePeriod(ym.atDay(1), ym.atEndOfMonth());
    }

    public boolean contains(LocalDate d){
        Guard.notNull(d, "date");
        return (d.equals(start) || d.isAfter(start)) && (d.equals(end) || d.isBefore(end));
    }

    public LocalDate start(){ return start; }
    public LocalDate end(){ return end; }
}
