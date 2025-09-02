package online.lifeasgame.user.domain.event;

import java.time.Instant;
import online.lifeasgame.core.event.DomainEvent;

public record UserRegistered(
        Long userId,
        String email,
        String nickname,
        Instant occurredAt
) implements DomainEvent {
    public static UserRegistered of(Long userId, String email, String nickname) {
        return new UserRegistered(userId, email, nickname, Instant.now());
    }
}
