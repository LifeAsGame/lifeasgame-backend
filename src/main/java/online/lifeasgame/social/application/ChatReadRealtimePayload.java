package online.lifeasgame.social.application;

public record ChatReadRealtimePayload(String eventType, Long channelId, Long playerId, Long lastReadMessageId) {
    public ChatReadRealtimePayload(Long channelId, Long playerId, Long lastReadMessageId) {
        this("READ_UPDATED", channelId, playerId, lastReadMessageId);
    }
}
