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

    private final ChatChannelJpaRepository jpaRepository;

    @Override
    public ChatChannel save(ChatChannel channel) {
        return jpaRepository.save(channel);
    }

    @Override
    public Optional<ChatChannel> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<ChatChannel> findByContext(ChatChannelType type, Long contextId) {
        return jpaRepository.findByTypeAndContextId(type, contextId);
    }

    @Override
    public Optional<ChatChannel> findByTypeAndName(ChatChannelType type, String name) {
        return jpaRepository.findByTypeAndNameValue(type, name);
    }

    @Override
    public Optional<ChatChannel> findFriendChannel(Long playerId, Long targetId) {
        return jpaRepository.findFriendChannel(ChatChannelType.FRIEND, playerId, targetId);
    }
}
