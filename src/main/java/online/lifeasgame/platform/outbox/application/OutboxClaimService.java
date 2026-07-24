package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.platform.outbox.OutboxProperties;
import online.lifeasgame.platform.outbox.domain.OutboxEvent;
import online.lifeasgame.platform.outbox.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<OutboxClaim> claimBatch() {
        Instant now = clock.instant();
        List<OutboxEvent> events = repository.findClaimableForUpdate(
                now,
                properties.getBatchSize()
        );
        events.forEach(event ->
                event.claim(properties.getInstanceId(), now)
        );
        repository.saveAllAndFlush(events);
        return events.stream()
                .map(event -> new OutboxClaim(
                        event.getId(),
                        event.getEventId(),
                        event.getEventType(),
                        event.getPayload(),
                        event.getLockedBy()
                ))
                .toList();
    }
}
