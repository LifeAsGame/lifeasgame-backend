package online.lifeasgame.character.application;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class EquipmentGrantReconciliation {
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactions;
    private final EquipmentAchievementGrant grants;
    private final AdminAuditInternalApi audit;

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
        long nextAfterPlayerId = afterPlayerId;
        Mode mode = apply ? Mode.APPLY : Mode.DRY_RUN;
        String correlationId = UUID.randomUUID().toString();
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (Long playerId : players) {
            try {
                eligible += outstanding(playerId);
                if (apply) {
                    issued += transaction.execute(status ->
                            grantAndAudit(playerId, "ACH_FIRST_LIFELOG", correlationId)
                                    + grantAndAudit(playerId, "ACH_ROUTE_BACKEND_START", correlationId));
                }
                nextAfterPlayerId = playerId;
            } catch (RuntimeException failure) {
                log.warn("Equipment reconciliation stopped at Player {} ({})",
                        playerId, failure.getClass().getSimpleName());
                // The failed Player transaction rolled back; the cursor must not skip it.
                return new Batch(mode, players.size(), eligible, issued,
                        nextAfterPlayerId, true, List.of(playerId));
            }
        }
        return new Batch(mode, players.size(), eligible, issued,
                nextAfterPlayerId, hasMore, List.of());
    }

    private int grantAndAudit(Long playerId, String achievementCode, String correlationId) {
        if (!grants.grantIfEligible(playerId, achievementCode)) return 0;
        audit.append(new AdminAuditInternalApi.AppendCommand(
                new AdminAuditAction("EQUIPMENT_GRANT_RECONCILE"),
                new AdminAuditTargetType("PLAYER_EQUIPMENT_GRANT"),
                playerId + ":" + achievementCode,
                "Reconcile approved achievement equipment entitlement",
                AdminAuditResult.SUCCESS, correlationId, null));
        return 1;
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

    public enum Mode { DRY_RUN, APPLY }

    public record Batch(Mode mode, int candidatePlayers, int eligibleGrants,
                        int issuedGrants, long nextAfterPlayerId, boolean hasMore,
                        List<Long> failedPlayerIds) {}
}
