package online.lifeasgame.social.application;

import online.lifeasgame.social.domain.ChatMessage;

import java.time.Instant;

public record ChatRealtimePayload(
        Long id,
        Long channelId,
        Long senderId,
        String content,
        boolean edited,
        Instant createdAt
) {

    public static ChatRealtimePayload from(ChatMessage message) {
        return new ChatRealtimePayload(
                message.getId(),
                message.getChannel().getId(),
                message.getSenderId(),
                message.getContent(),
                message.isEdited(),
                message.getCreatedAt()
        );
    }
}
