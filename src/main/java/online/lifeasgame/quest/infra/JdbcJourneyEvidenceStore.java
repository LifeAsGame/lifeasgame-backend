package online.lifeasgame.quest.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.JourneyEvidence;
import online.lifeasgame.quest.domain.repository.JourneyEvidenceStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class JdbcJourneyEvidenceStore implements JourneyEvidenceStore {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<JourneyEvidence> find(Long acceptanceId) {
        return jdbc.query("""
                SELECT acceptance_id, kind, life_log_id, memo, url, description, linked_at
                FROM quest_journey_evidence WHERE acceptance_id = ?
                """, (rs, row) -> new JourneyEvidence(
                        rs.getLong("acceptance_id"), rs.getString("kind"),
                        rs.getObject("life_log_id", Long.class), rs.getString("memo"),
                        rs.getString("url"), rs.getString("description"),
                        rs.getTimestamp("linked_at").toInstant()
                ), acceptanceId).stream().findFirst();
    }

    @Override
    public void insert(JourneyEvidence evidence) {
        jdbc.update("""
                INSERT INTO quest_journey_evidence
                (acceptance_id, kind, life_log_id, memo, url, description, linked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, evidence.acceptanceId(), evidence.kind(), evidence.lifeLogId(),
                evidence.memo(), evidence.url(), evidence.description(),
                Timestamp.from(evidence.linkedAt()));
    }

    @Override
    public void delete(Long acceptanceId) {
        jdbc.update("DELETE FROM quest_journey_evidence WHERE acceptance_id = ?", acceptanceId);
    }
}
