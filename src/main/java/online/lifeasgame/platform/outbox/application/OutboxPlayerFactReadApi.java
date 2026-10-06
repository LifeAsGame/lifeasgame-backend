package online.lifeasgame.platform.outbox.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.platform.outbox.application.codec.OutboxEventCodecRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Provider-owned read of retained durable facts for bounded reconciliation. */
@Component
@RequiredArgsConstructor
public class OutboxPlayerFactReadApi {
    private final JdbcTemplate jdbc;
    private final OutboxEventCodecRegistry codecs;

    @Transactional(readOnly = true)
    public List<OutboxEventDelivery> retainedFacts(Long playerId) {
        return jdbc.query("""
                SELECT event_id, event_type, payload FROM outbox_events
                WHERE event_type IN ('lifelog.recorded.v1', 'quest.event.v1',
                    'quest.route-completed.v1', 'inventory.item-reward-claimed.v1')
                  AND CAST(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.playerId')) AS UNSIGNED) = ?
                ORDER BY id
                """, (rs, row) -> new OutboxEventDelivery(rs.getString(1),
                codecs.decode(rs.getString(2), rs.getString(3))), playerId);
    }
}
