package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.ChatMessage;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage message);

    MessageSlice fetchMessages(Long channelId, Long cursor, int size);

    Optional<ChatMessage> findByClientMessageId(Long channelId, Long senderId, String clientMessageId);

    boolean existsByChannelIdAndId(Long channelId, Long messageId);

    long countUnread(Long channelId, Long playerId, Long lastReadMessageId);

    Map<Long, Long> unreadCounts(Long playerId, Set<Long> channelIds);

    record MessageSlice(List<ChatMessage> messages, boolean hasMore, Long nextCursor) {}
}
