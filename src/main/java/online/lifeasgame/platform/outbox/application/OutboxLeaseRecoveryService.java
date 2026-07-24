package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.platform.outbox.OutboxProperties;
import online.lifeasgame.platform.outbox.domain.OutboxEvent;
import online.lifeasgame.platform.outbox.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxLeaseRecoveryService {

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recoverStale() {
        Instant now = clock.instant();
        Instant expiredBefore = now.minus(
                Duration.ofMillis(properties.getLeaseDurationMs())
        );
        List<OutboxEvent> events = repository.findExpiredLeasesForUpdate(
                expiredBefore,
                properties.getBatchSize()
        );
        events.forEach(event -> event.recoverExpiredLease(now));
        repository.saveAllAndFlush(events);
        return events.size();
    }
}
