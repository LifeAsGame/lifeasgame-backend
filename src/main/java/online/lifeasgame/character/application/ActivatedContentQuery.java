package online.lifeasgame.character.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivatedContentQuery {
    private static final List<Definition> DEFINITIONS = List.of(
            new Definition("ACHIEVEMENT", "ACH_FIRST_LIFELOG", "내용이 완성된 첫 사용자 기록을 남기세요."),
            new Definition("ACHIEVEMENT", "ACH_FIRST_QUEST_COMPLETE", "첫 퀘스트를 실제로 완료하세요."),
            new Definition("ACHIEVEMENT", "ACH_FIRST_ITEM_CLAIM", "우편 아이템을 Inventory로 수령하세요."),
            new Definition("ACHIEVEMENT", "ACH_ROUTE_RECORD_START", "기록 여정의 마지막 단계를 직접 진행하세요."),
            new Definition("ACHIEVEMENT", "ACH_ROUTE_BACKEND_START", "백엔드 개발자 여정의 마지막 단계를 직접 진행하세요."),
            new Definition("TITLE", "TITLE_CANDIDATE_RECORD_BEGINNER", "첫 기록 업적을 얻으세요."),
            new Definition("TITLE", "TITLE_BACKEND_GUIDE", "백엔드 개발자 여정 완주 업적을 얻으세요.")
    );

    private final JdbcTemplate jdbc;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional(readOnly = true)
    public Page list(int page, int size) {
        if (page < 0 || size < 1 || size > 20) {
            throw new IllegalArgumentException("Invalid activation page");
        }
        long offset = (long) page * size;
        if (offset >= DEFINITIONS.size()) return new Page(List.of(), page, size, false);
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<Entry> entries = new ArrayList<>();
        int end = (int) Math.min(DEFINITIONS.size(), offset + size);
        for (int i = (int) offset; i < end; i++) {
            entries.add(load(playerId, DEFINITIONS.get(i)));
        }
        return new Page(List.copyOf(entries), page, size, end < DEFINITIONS.size());
    }

    private Entry load(Long playerId, Definition definition) {
        if (definition.kind.equals("ACHIEVEMENT")) {
            return jdbc.queryForObject("""
                    SELECT a.id, a.name, a.definition_version, pa.acquired_at,
                           r.status receipt_status, r.source_occurred_at
                    FROM achievements a
                    LEFT JOIN player_achievements pa
                      ON pa.achievement_id = a.id AND pa.player_id = ?
                    LEFT JOIN achievement_award_receipts r
                      ON r.player_id = ? AND r.achievement_code = a.code
                    WHERE a.code = ?
                    """, (rs, row) -> map(definition, rs), playerId, playerId, definition.code);
        }
        return jdbc.queryForObject("""
                SELECT t.id, t.name, t.definition_version, pt.acquired_at,
                       CASE WHEN pt.acquired_at IS NULL AND b.player_id IS NOT NULL
                            THEN 'REVOKED'
                            WHEN r.status = 'GRANTED' AND pt.acquired_at = r.processed_at
                            THEN 'GRANTED' ELSE NULL END receipt_status,
                       CASE WHEN r.status = 'GRANTED' AND pt.acquired_at = r.processed_at
                            THEN r.source_occurred_at ELSE NULL END source_occurred_at
                FROM titles t
                LEFT JOIN player_titles pt ON pt.title_id = t.id AND pt.player_id = ?
                LEFT JOIN title_award_blocks b ON b.player_id = ? AND b.title_code = t.code
                LEFT JOIN achievement_award_receipts r ON r.player_id = ?
                  AND r.linked_title_code = t.code
                WHERE t.code = ?
                """, (rs, row) -> map(definition, rs), playerId, playerId, playerId, definition.code);
    }

    private Entry map(Definition definition, ResultSet rs) throws SQLException {
        Instant acquiredAt = timestamp(rs, "acquired_at");
        String receipt = rs.getString("receipt_status");
        String evidence = acquiredAt == null
                ? "REVOKED".equals(receipt) ? "REVOKED" : "NONE"
                : "GRANTED".equals(receipt) ? "CONFIRMED" : "ADMIN_OR_LEGACY";
        return new Entry(definition.kind, definition.code, rs.getLong("id"),
                rs.getString("name"), rs.getInt("definition_version"),
                definition.condition, acquiredAt == null ? "UNACQUIRED" : "ACQUIRED",
                evidence, acquiredAt,
                "CONFIRMED".equals(evidence) ? timestamp(rs, "source_occurred_at") : null);
    }

    private static Instant timestamp(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private record Definition(String kind, String code, String condition) {}
    public record Entry(String kind, String code, Long definitionId, String name,
                        int definitionVersion, String condition, String status,
                        String evidenceStatus, Instant acquiredAt, Instant sourceOccurredAt) {}
    public record Page(List<Entry> entries, int page, int size, boolean hasNext) {}
}
