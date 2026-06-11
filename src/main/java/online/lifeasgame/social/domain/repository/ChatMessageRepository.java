package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.ChatMessage;

import java.util.List;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage message);

    MessageSlice fetchMessages(Long channelId, Long cursor, int size);

    record MessageSlice(List<ChatMessage> messages, boolean hasMore, Long nextCursor) {}
}
