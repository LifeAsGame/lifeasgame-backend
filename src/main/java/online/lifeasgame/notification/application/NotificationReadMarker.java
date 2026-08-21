package online.lifeasgame.notification.application;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.notification.domain.PlayerNotification;
import online.lifeasgame.notification.domain.error.NotificationError;
import online.lifeasgame.notification.domain.repository.PlayerNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationReadMarker {

    private final PlayerNotificationRepository repository;
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final Clock clock;

    @Transactional
    public void markOne(Long notificationId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        PlayerNotification notification = repository.findOwned(
                        notificationId,
                        playerId
                )
                .orElseThrow(() -> new DomainException(
                        NotificationError.NOTIFICATION_NOT_FOUND
                ));
        notification.markRead(clock.instant());
    }

    @Transactional
    public int markAll() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Instant now = clock.instant();
        return repository.markAllUnread(playerId, now);
    }
}
