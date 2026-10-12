package online.lifeasgame.quest.application.internal;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Provider-confirmed historical completion state; no Quest reward path is invoked. */
@Component
@RequiredArgsConstructor
public class QuestAchievementEvidenceApi {
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<CompletedAcceptance> completedAcceptances(Long playerId) {
        return jdbc.query("""
                SELECT id, completed_at FROM quest_acceptances
                WHERE player_id = ? AND status = 'DONE' AND completed_at IS NOT NULL
                ORDER BY completed_at, id
                LIMIT 1
                """, (rs, row) -> new CompletedAcceptance(rs.getLong(1),
                rs.getTimestamp(2).toInstant()), playerId);
    }

    @Transactional(readOnly = true)
    public List<CompletedRoute> completedRoutes(Long playerId) {
        return jdbc.query("""
                SELECT pqr.id, qr.code, pqr.completed_at
                FROM player_quest_routes pqr JOIN quest_routes qr ON qr.id = pqr.route_id
                WHERE pqr.player_id = ? AND pqr.status = 'COMPLETED'
                  AND pqr.completed_at IS NOT NULL
                  AND qr.code IN ('ROUTE_RECORD_START', 'ROUTE_BACKEND_DEVELOPER_START')
                ORDER BY pqr.completed_at, pqr.id
                """, (rs, row) -> new CompletedRoute(rs.getLong(1),
                rs.getString(2), rs.getTimestamp(3).toInstant()), playerId);
    }

    public record CompletedAcceptance(Long id, Instant completedAt) {}
    public record CompletedRoute(Long id, String routeCode, Instant completedAt) {}
}
