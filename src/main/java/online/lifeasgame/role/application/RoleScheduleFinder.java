package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.core.time.CalendarDateRange;
import online.lifeasgame.role.application.query.RoleScheduleQuery;
import online.lifeasgame.role.application.query.RoleScheduleQuery.Criteria;
import online.lifeasgame.role.application.query.RoleScheduleQuery.Source;
import online.lifeasgame.role.application.query.RoleScheduleQuery.Status;
import online.lifeasgame.role.application.query.RoleScheduleQuery.TimeMode;
import online.lifeasgame.role.application.result.RoleScheduleResult;
import online.lifeasgame.role.domain.error.RoleError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class RoleScheduleFinder {
    private static final ZoneId CALENDAR_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration MAX_WINDOW = Duration.ofDays(366);

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final RoleReader roles;
    private final RoleScheduleQuery schedule;
    private final Clock clock;

    public RoleScheduleResult.Page find(Long roleId, Instant from, Instant to, TimeMode time,
                                        Source source, Status status, boolean participating,
                                        int page, int size) {
        Long owner = currentPlayerAccessor.currentPlayerIdOrThrow();
        roles.getOwned(roleId, owner);
        if (page < 0 || page > 1000 || size < 1 || size > 50 ||
                time == null || source == null || status == null ||
                participating && (source == Source.ALL || source == Source.PERSONAL || time != TimeMode.DATED) ||
                time == TimeMode.UNSCHEDULED && (from != null || to != null || source != Source.ALL && source != Source.PERSONAL) ||
                (from == null) != (to == null)) invalid();

        if (time == TimeMode.DATED) {
            if (from == null) {
                LocalDate first = LocalDate.now(clock.withZone(CALENDAR_ZONE)).withDayOfMonth(1);
                from = first.atStartOfDay(CALENDAR_ZONE).toInstant();
                to = first.plusMonths(1).atStartOfDay(CALENDAR_ZONE).toInstant();
            }
            if (!CalendarDateRange.inDateTimeColumn(from) || !CalendarDateRange.inDateTimeColumn(to) ||
                    !from.isBefore(to) || Duration.between(from, to).compareTo(MAX_WINDOW) > 0) invalid();
        }
        return schedule.find(owner, roleId, new Criteria(from, to, time, source, status,
                participating, page, size));
    }

    private static void invalid() { throw new DomainException(RoleError.INVALID_ROLE_SCHEDULE_QUERY); }
}
