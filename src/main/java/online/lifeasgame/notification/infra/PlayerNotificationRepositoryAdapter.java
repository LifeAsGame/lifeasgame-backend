package online.lifeasgame.notification.infra;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.notification.domain.PlayerNotification;
import online.lifeasgame.notification.domain.repository.PlayerNotificationRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlayerNotificationRepositoryAdapter
        implements PlayerNotificationRepository {

    private final JpaPlayerNotificationRepository repository;

    @Override
    public PlayerNotification saveAndFlush(PlayerNotification notification) {
        return repository.saveAndFlush(notification);
    }

    @Override
    public boolean existsByPlayerIdAndSourceEventId(
            Long playerId,
            String sourceEventId
    ) {
        return repository.existsByPlayerIdAndSourceEventId(
                playerId,
                sourceEventId
        );
    }

    @Override
    public Optional<PlayerNotification> findOwned(
            Long notificationId,
            Long playerId
    ) {
        return repository.findByIdAndPlayerId(notificationId, playerId);
    }

    @Override
    public int markAllUnread(Long playerId, Instant readAt) {
        return repository.markAllUnread(playerId, readAt);
    }
}
