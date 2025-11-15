package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.ChatMessage;
import online.lifeasgame.social.domain.repository.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryAdapter implements ChatMessageRepository {

    private final ChatMessageJpaRepository chatMessageJpaRepository;

    @Override
    public ChatMessage save(ChatMessage message) {
        return chatMessageJpaRepository.save(message);
    }

    @Override
    public MessageSlice fetchMessages(Long channelId, Long cursor, int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        List<ChatMessage> fetched = chatMessageJpaRepository.findMessages(
                channelId,
                cursor,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasMore = fetched.size() > pageSize;
        List<ChatMessage> trimmed = new ArrayList<>(hasMore ? fetched.subList(0, pageSize) : fetched);
        Collections.reverse(trimmed);
        Long nextCursor = hasMore && !trimmed.isEmpty() ? trimmed.get(0).getId() : null;
        return new MessageSlice(trimmed, hasMore, nextCursor);
    }
}

