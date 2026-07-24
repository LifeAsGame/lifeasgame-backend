package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.outbox.OutboxProperties;
import online.lifeasgame.platform.outbox.domain.OutboxEvent;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;
import online.lifeasgame.platform.outbox.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxFailureService {

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(OutboxClaim claim, RuntimeException failure) {
        OutboxEvent event = repository.findByIdForUpdate(claim.id())
                .orElseThrow(() ->
                        new DomainException(
                                OutboxError.OUTBOX_EVENT_NOT_FOUND
                        )
                );
        Instant now = clock.instant();
        event.markFailed(
                claim.lockedBy(),
                properties.getMaxAttempts(),
                now,
                now.plus(Duration.ofMillis(properties.getRetryDelayMs())),
                safeError(failure)
        );
        repository.save(event);
    }

    private String safeError(RuntimeException failure) {
        return "Dispatch failed (" + failure.getClass().getSimpleName() + ")";
    }
}
