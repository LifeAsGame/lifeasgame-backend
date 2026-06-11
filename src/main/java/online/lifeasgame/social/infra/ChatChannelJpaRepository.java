package online.lifeasgame.social.infra;

import online.lifeasgame.social.domain.ChatChannel;
import online.lifeasgame.social.domain.ChatChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatChannelJpaRepository extends JpaRepository<ChatChannel, Long> {

    Optional<ChatChannel> findByTypeAndContextId(ChatChannelType type, Long contextId);

    Optional<ChatChannel> findByTypeAndNameValue(ChatChannelType type, String name);

    List<ChatChannel> findByIdIn(Collection<Long> ids);

    @Query(
            """
                SELECT DISTINCT c
                FROM ChatChannel c
                JOIN ChannelParticipant p1 ON p1.channel = c AND p1.userId = :playerId
                JOIN ChannelParticipant p2 ON p2.channel = c AND p2.userId = :targetId
                WHERE c.type = :type
            """
    )
    Optional<ChatChannel> findFriendChannel(
            @Param("type") ChatChannelType type,
            @Param("playerId") Long playerId,
            @Param("targetId") Long targetId
    );
}
