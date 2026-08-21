package online.lifeasgame.notification.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.notification.domain.repository.PlayerNotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class NotificationFinder {

    private final PlayerNotificationRepository repository;

    public boolean exists(Long playerId, String sourceEventId) {
        return repository.existsByPlayerIdAndSourceEventId(playerId, sourceEventId);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean existsInNewTransaction(Long playerId, String sourceEventId) {
        return repository.existsByPlayerIdAndSourceEventId(playerId, sourceEventId);
    }
}
