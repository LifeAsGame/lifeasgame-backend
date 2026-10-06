package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AchievementReconciliation {
    private final JdbcTemplate jdbc;
    private final AchievementReconciliationWorker worker;

    public Batch run(long afterPlayerId, int batchSize, boolean apply) {
        if (afterPlayerId < 0 || batchSize < 1 || batchSize > 100) {
            throw new IllegalArgumentException("Invalid reconciliation bounds");
        }
        List<Long> playerIds = jdbc.query("""
                SELECT id FROM player WHERE id > ? ORDER BY id LIMIT ?
                """, (rs, row) -> rs.getLong(1), afterPlayerId, batchSize + 1);
        boolean hasMore = playerIds.size() > batchSize;
        if (hasMore) playerIds = playerIds.subList(0, batchSize);
        List<AchievementReconciliationWorker.PlayerResult> results = playerIds.stream()
                .map(playerId -> worker.process(playerId, apply)).toList();
        long nextAfterPlayerId = playerIds.isEmpty()
                ? afterPlayerId : playerIds.getLast();
        return new Batch(apply ? "APPLIED" : "DRY_RUN", nextAfterPlayerId,
                hasMore, results);
    }

    public record Batch(String mode, long nextAfterPlayerId, boolean hasMore,
                        List<AchievementReconciliationWorker.PlayerResult> players) {}
}
