package online.lifeasgame.platform.outbox.domain.repository;

import online.lifeasgame.platform.outbox.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> saveAllAndFlush(List<OutboxEvent> events);

    List<OutboxEvent> findClaimableForUpdate(Instant now, int batchSize);

    List<OutboxEvent> findExpiredLeasesForUpdate(
            Instant expiredBefore,
            int batchSize
    );

    Optional<OutboxEvent> findByIdForUpdate(Long id);

    Optional<OutboxEvent> findById(Long id);
}
