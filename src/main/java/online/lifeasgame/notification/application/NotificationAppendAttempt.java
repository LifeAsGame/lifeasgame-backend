package online.lifeasgame.notification.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.notification.domain.PlayerNotification;
import online.lifeasgame.notification.domain.repository.PlayerNotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationAppendAttempt {

    private final PlayerNotificationRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(PlayerNotification notification) {
        repository.saveAndFlush(notification);
    }
}
