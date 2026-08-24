package online.lifeasgame.adminaudit.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditEvent;
import online.lifeasgame.adminaudit.domain.repository.AdminAuditEventRepository;
import online.lifeasgame.core.security.CurrentUserAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminAuditAppender implements AdminAuditInternalApi {

    private final AdminAuditEventRepository repository;
    private final CurrentUserAccessor currentUserAccessor;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AppendResult append(AppendCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant occurredAt = Instant.now(clock);
        AdminAuditEvent event = repository.append(AdminAuditEvent.record(
                currentUserAccessor.currentUserIdOrThrow(),
                command.action(),
                command.targetType(),
                command.targetId(),
                command.reason(),
                command.result(),
                command.correlationId(),
                command.idempotencyKey(),
                occurredAt
        ));
        return new AppendResult(event.getId(), occurredAt);
    }
}
