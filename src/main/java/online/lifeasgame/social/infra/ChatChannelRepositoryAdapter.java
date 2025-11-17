package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.ChatChannel;
import online.lifeasgame.social.domain.ChatChannelType;
import online.lifeasgame.social.domain.repository.ChatChannelRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatChannelRepositoryAdapter implements ChatChannelRepository {

    private final ChatChannelJpaRepository chatChannelJpaRepository;

    @Override
    public ChatChannel save(ChatChannel channel) {
        return chatChannelJpaRepository.save(channel);
    }

    @Override
    public Optional<ChatChannel> findById(Long id) {
        return chatChannelJpaRepository.findById(id);
    }

    @Override
    public Optional<ChatChannel> findByContext(ChatChannelType type, Long contextId) {
        return chatChannelJpaRepository.findByTypeAndContextId(type, contextId);
    }

    @Override
    public Optional<ChatChannel> findByTypeAndName(ChatChannelType type, String name) {
        return chatChannelJpaRepository.findByTypeAndNameValue(type, name);
    }

    @Override
    public Optional<ChatChannel> findFriendChannel(Long playerId, Long targetId) {
        return chatChannelJpaRepository.findFriendChannel(ChatChannelType.FRIEND, playerId, targetId);
    }
}
