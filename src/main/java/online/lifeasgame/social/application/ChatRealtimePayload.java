package online.lifeasgame.social.application;

import online.lifeasgame.social.domain.ChatMessage;

import java.time.Instant;

public record ChatRealtimePayload(
        String eventType,
        Long id,
        Long channelId,
        Long senderId,
        String content,
        boolean edited,
        Instant createdAt,
        String clientMessageId
) {

    public static ChatRealtimePayload from(ChatMessage message) {
        return new ChatRealtimePayload(
                "MESSAGE_CREATED",
                message.getId(),
                message.getChannel().getId(),
                message.getSenderId(),
                message.getContent(),
                message.isEdited(),
                message.getCreatedAt(),
                message.getClientMessageId()
        );
    }
}
