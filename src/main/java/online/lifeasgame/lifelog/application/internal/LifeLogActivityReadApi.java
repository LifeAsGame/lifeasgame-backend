package online.lifeasgame.lifelog.application.internal;

import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;

import java.time.Instant;
import java.util.List;

public interface LifeLogActivityReadApi {

    List<LifeLogJournalResult.Entry> recentJournal(
            Long playerId,
            int limit
    );

    RoleActivity roleActivity(
            Long playerId,
            Instant windowStart,
            Instant windowEnd
    );

    record RoleActivity(
            long totalRecords,
            long assignedRecords,
            long unassignedRecords,
            List<RoleCount> roles
    ) {
    }

    record RoleCount(Long roleId, long recordCount) {
    }
}
