package online.lifeasgame.character.application.query;

import java.time.Instant;
import java.util.List;

public interface GrowthQuery {

    List<RecentExpChange> findRecentExpChanges(Long playerId, int limit);

    record RecentExpChange(
            Long changeId,
            Long rewardLineId,
            long requestedExp,
            long appliedExp,
            long leftoverExp,
            int beforeLevel,
            int afterLevel,
            long beforeTotalExp,
            long afterTotalExp,
            Instant occurredAt
    ) {
    }
}
