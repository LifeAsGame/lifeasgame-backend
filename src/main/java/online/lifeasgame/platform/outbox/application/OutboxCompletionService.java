package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.outbox.domain.OutboxEvent;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;
import online.lifeasgame.platform.outbox.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class OutboxCompletionService {

    private final OutboxEventRepository repository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(OutboxClaim claim) {
        OutboxEvent event = repository.findByIdForUpdate(claim.id())
                .orElseThrow(() ->
                        new DomainException(
                                OutboxError.OUTBOX_EVENT_NOT_FOUND
                        )
                );
        event.markPublished(claim.lockedBy(), clock.instant());
        repository.save(event);
    }
}
