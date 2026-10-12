package online.lifeasgame.character.infra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

/** Reads grant times only where the receipt identifies the exact ownership row. */
@Repository
@RequiredArgsConstructor
class AwardReceiptAcquiredAtQuery {
    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Seoul");

    private final JdbcTemplate jdbc;

    Map<Long, Instant> achievements(Long playerId) {
        return jdbc.query("""
                SELECT pa.achievement_id definition_id, r.processed_at
                FROM player_achievements pa
                JOIN achievements a ON a.id = pa.achievement_id
                JOIN achievement_award_receipts r ON r.player_id = pa.player_id
                  AND r.achievement_code = a.code AND r.status = 'GRANTED'
                  AND r.processed_at = pa.acquired_at
                WHERE pa.player_id = ?
                """, (ResultSetExtractor<Map<Long, Instant>>) AwardReceiptAcquiredAtQuery::times,
                playerId);
    }

    Map<Long, Instant> titles(Long playerId) {
        return jdbc.query("""
                SELECT pt.title_id definition_id, r.processed_at
                FROM player_titles pt
                JOIN titles t ON t.id = pt.title_id
                JOIN achievement_award_receipts r ON r.player_id = pt.player_id
                  AND r.linked_title_code = t.code AND r.status = 'GRANTED'
                  AND r.processed_at = pt.acquired_at
                WHERE pt.player_id = ?
                """, (ResultSetExtractor<Map<Long, Instant>>) AwardReceiptAcquiredAtQuery::times,
                playerId);
    }

    private static Map<Long, Instant> times(ResultSet rs) throws SQLException {
        Map<Long, Instant> result = new HashMap<>();
        while (rs.next()) {
            result.put(rs.getLong("definition_id"),
                    rs.getObject("processed_at", LocalDateTime.class)
                            .atZone(DATABASE_ZONE).toInstant());
        }
        return result;
    }
}
