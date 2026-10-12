package online.lifeasgame.character.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.platform.outbox.application.OutboxEventDelivery;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Awards a justified fact inside the durable delivery transaction. */
@Component
@RequiredArgsConstructor
public class AchievementFactProcessor {
    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Seoul");
    private final PlayerReader playerReader;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onDelivery(OutboxEventDelivery delivery) {
        AchievementActivationRule.match(delivery.event()).ifPresent(fact -> {
            // Retained pre-Player fixture/orphan facts cannot create Player ownership.
            Integer players = jdbc.queryForObject("SELECT COUNT(*) FROM player WHERE id = ?",
                    Integer.class, fact.playerId());
            if (players != null && players > 0) {
                award(fact, delivery.eventId(), "DURABLE_EVENT");
            }
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void award(AchievementActivationRule.EligibleFact fact,
                      String sourceEventId, String provenance) {
        // Every character grant/revoke/representative command takes this lock first.
        playerReader.getByIdForUpdateOrThrow(fact.playerId());
        Integer receipts = jdbc.queryForObject("""
                SELECT COUNT(*) FROM achievement_award_receipts
                WHERE player_id = ? AND achievement_code = ?
                """, Integer.class, fact.playerId(), fact.achievementCode());
        if (receipts != null && receipts > 0) return;

        Long achievementId = requiredDefinition("achievements", fact.achievementCode());
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), DATABASE_ZONE);
        boolean alreadyOwned = owned("player_achievements", "achievement_id",
                fact.playerId(), achievementId);
        if (!alreadyOwned) {
            jdbc.update("""
                    INSERT INTO player_achievements
                    (player_id, achievement_id, acquired_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, fact.playerId(), achievementId, now, now, now);
        }

        if (!alreadyOwned && fact.linkedTitleCode() != null
                && !titleBlocked(fact.playerId(), fact.linkedTitleCode())) {
            Long titleId = requiredDefinition("titles", fact.linkedTitleCode());
            if (!owned("player_titles", "title_id", fact.playerId(), titleId)) {
                jdbc.update("""
                        INSERT INTO player_titles
                        (player_id, title_id, acquired_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?)
                        """, fact.playerId(), titleId, now, now, now);
            }
        }

        jdbc.update("""
                INSERT INTO achievement_award_receipts
                (player_id, achievement_code, source_kind, source_key,
                 source_event_id, source_occurred_at, provenance, rule_version,
                 processed_at, status, linked_title_code, title_revoked)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)
                """, fact.playerId(), fact.achievementCode(), fact.sourceKind(),
                fact.sourceKey(), sourceEventId,
                LocalDateTime.ofInstant(fact.occurredAt(), DATABASE_ZONE),
                provenance, now, alreadyOwned ? "EXISTING" : "GRANTED",
                fact.linkedTitleCode(), titleBlocked(fact.playerId(), fact.linkedTitleCode()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markAchievementRevoked(Long playerId, String code) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), DATABASE_ZONE);
        jdbc.update("""
                INSERT INTO achievement_award_receipts
                (player_id, achievement_code, rule_version, processed_at, status, title_revoked)
                VALUES (?, ?, 1, ?, 'REVOKED', FALSE)
                ON DUPLICATE KEY UPDATE status = 'REVOKED'
                """, playerId, code, now);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markTitleRevoked(Long playerId, String code) {
        jdbc.update("""
                INSERT INTO title_award_blocks (player_id, title_code, revoked_at)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE revoked_at = VALUES(revoked_at)
                """, playerId, code,
                LocalDateTime.ofInstant(clock.instant(), DATABASE_ZONE));
        jdbc.update("""
                UPDATE achievement_award_receipts SET title_revoked = TRUE
                WHERE player_id = ? AND linked_title_code = ?
                """, playerId, code);
    }

    private Long requiredDefinition(String table, String code) {
        // Table identifiers are fixed internal literals, never caller input.
        var ids = jdbc.query("SELECT id FROM " + table + " WHERE code = ?",
                (rs, row) -> rs.getLong(1), code);
        if (ids.size() != 1) throw new IllegalStateException("Activation definition missing: " + code);
        return ids.getFirst();
    }

    private boolean owned(String table, String idColumn, Long playerId, Long definitionId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table
                + " WHERE player_id = ? AND " + idColumn + " = ?",
                Integer.class, playerId, definitionId);
        return count != null && count > 0;
    }

    private boolean titleBlocked(Long playerId, String titleCode) {
        if (titleCode == null) return false;
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM title_award_blocks
                WHERE player_id = ? AND title_code = ?
                """, Integer.class, playerId, titleCode);
        return count != null && count > 0;
    }
}
