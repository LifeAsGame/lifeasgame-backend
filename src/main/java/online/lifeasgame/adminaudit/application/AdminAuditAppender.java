package online.lifeasgame.adminaudit.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditEvent;
import online.lifeasgame.adminaudit.domain.error.AdminAuditError;
import online.lifeasgame.adminaudit.domain.repository.AdminAuditEventRepository;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.sql.SQLException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminAuditAppender implements AdminAuditInternalApi {

    private final AdminAuditEventRepository repository;
    private final CurrentUserAccessor currentUserAccessor;
    private final UserAuthApi userAuthApi;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AppendResult append(AppendCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant occurredAt = Instant.now(clock);
        AdminAuditEvent event;
        try {
            event = repository.append(AdminAuditEvent.record(
                    currentAdminId(),
                    command.action(),
                    command.targetType(),
                    command.targetId(),
                    command.reason(),
                    command.result(),
                    command.correlationId(),
                    command.idempotencyKey(),
                    occurredAt
            ));
        } catch (DataIntegrityViolationException exception) {
            if (command.idempotencyKey() != null && isDuplicate(exception)) {
                throw new DomainException(
                        AdminAuditError.DUPLICATE_IDEMPOTENCY_KEY
                );
            }
            throw exception;
        }
        return new AppendResult(event.getId(), occurredAt);
    }

    private Long currentAdminId() {
        Long userId = currentUserAccessor.currentUserIdOrThrow();
        return userAuthApi.resolveAuthorization(userId)
                .filter(UserAuthApi.AccountAuthorization::active)
                .filter(UserAuthApi.AccountAuthorization::admin)
                .map(authorization -> userId)
                .orElseThrow(() -> new AuthException(AuthError.FORBIDDEN));
    }

    private boolean isDuplicate(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && sqlException.getErrorCode() == 1062) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
