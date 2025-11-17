package online.lifeasgame.social.domain.event;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.social.domain.ChatChannelType;

import java.time.Instant;

public record ChatChannelDeactivated(
        Long channelId, ChatChannelType chatChannelType, Instant occurredAt, String reason
) implements DomainEvent {
}