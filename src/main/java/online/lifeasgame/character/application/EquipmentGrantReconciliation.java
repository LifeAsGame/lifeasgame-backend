package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class EquipmentGrantReconciliation {
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactions;
    private final EquipmentAchievementGrant grants;

    public Batch run(long afterPlayerId, int batchSize, boolean apply) {
        if (afterPlayerId < 0 || batchSize < 1 || batchSize > 100) {
            throw new IllegalArgumentException("Invalid reconciliation bounds");
        }
        List<Long> players = jdbc.query("""
                SELECT DISTINCT pa.player_id FROM player_achievements pa
                JOIN achievements a ON a.id = pa.achievement_id
                WHERE pa.player_id > ? AND a.code IN ('ACH_FIRST_LIFELOG', 'ACH_ROUTE_BACKEND_START')
                ORDER BY pa.player_id LIMIT ?
                """, (rs, row) -> rs.getLong(1), afterPlayerId, batchSize + 1);
        boolean hasMore = players.size() > batchSize;
        if (hasMore) players = players.subList(0, batchSize);
        int eligible = 0;
        int issued = 0;
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (Long playerId : players) {
            int before = outstanding(playerId);
            eligible += before;
            if (apply) {
                issued += transaction.execute(status -> {
                    int count = 0;
                    if (grants.grantIfEligible(playerId, "ACH_FIRST_LIFELOG")) count++;
                    if (grants.grantIfEligible(playerId, "ACH_ROUTE_BACKEND_START")) count++;
                    return count;
                });
            }
        }
        return new Batch(apply ? "APPLIED" : "DRY_RUN", players.size(), eligible,
                issued, players.isEmpty() ? afterPlayerId : players.getLast(), hasMore);
    }

    private int outstanding(Long playerId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM player_achievements pa
                JOIN achievements a ON a.id = pa.achievement_id
                WHERE pa.player_id = ? AND (
                    (a.code = 'ACH_FIRST_LIFELOG' AND NOT EXISTS (
                        SELECT 1 FROM player_equipment_grant_receipts r
                        WHERE r.player_id = pa.player_id AND r.entitlement_code = 'EQUIPMENT_RECORD_KEEPER_HAT'))
                    OR (a.code = 'ACH_ROUTE_BACKEND_START' AND NOT EXISTS (
                        SELECT 1 FROM player_equipment_grant_receipts r
                        WHERE r.player_id = pa.player_id AND r.entitlement_code = 'EQUIPMENT_DEVELOPER_HOODIE')))
                """, Integer.class, playerId);
    }

    public record Batch(String mode, int candidatePlayers, int eligibleGrants,
                        int issuedGrants, long nextAfterPlayerId, boolean hasMore) {}
}
