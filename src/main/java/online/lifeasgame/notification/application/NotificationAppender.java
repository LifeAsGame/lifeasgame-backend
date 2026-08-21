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
        PlayerNotification notification = PlayerNotification.create(
                command.playerId(),
                command.sourceEventId(),
                command.type(),
                command.title(),
                command.body(),
                command.occurredAt()
        );
        if (finder.exists(
                notification.getPlayerId(),
                notification.getSourceEventId()
        )) {
            return;
        }

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
