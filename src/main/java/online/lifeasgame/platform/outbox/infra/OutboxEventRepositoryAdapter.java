package online.lifeasgame.platform.outbox.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.platform.outbox.domain.OutboxEvent;
import online.lifeasgame.platform.outbox.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final JpaOutboxEventRepository repository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return repository.save(event);
    }

    @Override
    public List<OutboxEvent> saveAllAndFlush(List<OutboxEvent> events) {
        return repository.saveAllAndFlush(events);
    }

    @Override
    public List<OutboxEvent> findClaimableForUpdate(
            Instant now,
            int batchSize
    ) {
        return repository.findClaimableForUpdate(now, batchSize);
    }

    @Override
    public List<OutboxEvent> findExpiredLeasesForUpdate(
            Instant expiredBefore,
            int batchSize
    ) {
        return repository.findExpiredLeasesForUpdate(
                expiredBefore,
                batchSize
        );
    }

    @Override
    public Optional<OutboxEvent> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id);
    }

    @Override
    public Optional<OutboxEvent> findById(Long id) {
        return repository.findById(id);
    }
}
