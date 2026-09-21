package online.lifeasgame.notification.application;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.notification.application.internal.NotificationAppendApi;
import online.lifeasgame.notification.domain.PlayerNotification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationAppender implements NotificationAppendApi {

    private final NotificationFinder finder;
    private final NotificationAppendAttempt appendAttempt;

    @Override
    @Transactional
    public void append(AppendCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        // A replay preserves even legacy text/provenance; do not re-render stored notifications.
        if (finder.exists(command.playerId(), command.sourceEventId())) {
            return;
        }
        PlayerNotification notification = PlayerNotification.create(
                command.playerId(),
                command.sourceEventId(),
                command.type(),
                command.questTitle(),
                command.occurredAt()
        );
        try {
            appendAttempt.append(notification);
        } catch (DataIntegrityViolationException exception) {
            if (!finder.existsInNewTransaction(
                    notification.getPlayerId(),
                    notification.getSourceEventId()
            )) {
                throw exception;
            }
        }
    }
}
