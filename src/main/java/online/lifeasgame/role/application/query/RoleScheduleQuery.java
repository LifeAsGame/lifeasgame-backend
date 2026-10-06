package online.lifeasgame.role.application.query;

import online.lifeasgame.role.application.result.RoleScheduleResult;

import java.time.Instant;

/** Bounded cross-domain read projection. It never loads or changes foreign aggregates. */
public interface RoleScheduleQuery {
    RoleScheduleResult.Page find(Long owner, Long roleId, Criteria criteria);

    record Criteria(Instant from, Instant to, TimeMode time, Source source,
                    Status status, boolean participating, int page, int size) {}

    enum TimeMode { DATED, UNSCHEDULED }
    enum Source { ALL, PERSONAL, GUILD, PARTY, ROLE_PARTY, SHARED }
    enum Status { ALL, PLANNED, COMPLETED, CANCELED }
}
