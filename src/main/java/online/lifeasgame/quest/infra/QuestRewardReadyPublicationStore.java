package online.lifeasgame.quest.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class QuestRewardReadyPublicationStore {

    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(String parentEventId) {
        String claimToken = UUID.randomUUID().toString();
        // The unique insert serializes concurrent deliveries; row counts differ by JDBC settings.
        jdbc.update("""
                INSERT INTO quest_reward_ready_publications (parent_event_id, claim_token)
                VALUES (?, ?) ON DUPLICATE KEY UPDATE parent_event_id = parent_event_id
                """, parentEventId, claimToken);
        String owner = jdbc.queryForObject("""
                SELECT claim_token FROM quest_reward_ready_publications
                WHERE parent_event_id = ? FOR UPDATE
                """, String.class, parentEventId);
        return claimToken.equals(owner);
    }
}
