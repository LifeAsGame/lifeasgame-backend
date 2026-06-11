package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.ChatChannel;
import online.lifeasgame.social.domain.ChatChannelType;

import java.util.Optional;

public interface ChatChannelRepository {

    ChatChannel save(ChatChannel channel);

    Optional<ChatChannel> findById(Long id);

    Optional<ChatChannel> findByContext(ChatChannelType type, Long contextId);

    Optional<ChatChannel> findByTypeAndName(ChatChannelType type, String name);

    Optional<ChatChannel> findFriendChannel(Long playerId, Long targetId);
}
