package online.lifeasgame.social.infra;

import online.lifeasgame.social.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByChannelIdAndSenderIdAndClientMessageId(Long channelId, Long senderId, String clientMessageId);

    boolean existsByChannelIdAndId(Long channelId, Long id);

    @Query("select count(m) from ChatMessage m where m.channel.id = :channelId and m.senderId <> :playerId and (:lastReadMessageId is null or m.id > :lastReadMessageId)")
    long countUnread(@Param("channelId") Long channelId, @Param("playerId") Long playerId,
                     @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("select m.channel.id, count(m) from ChatMessage m join ChannelParticipant cp on cp.channel = m.channel and cp.userId = :playerId where m.channel.id in :channelIds and m.senderId <> :playerId and (cp.lastReadMessageId is null or m.id > cp.lastReadMessageId) group by m.channel.id")
    List<Object[]> unreadCounts(@Param("playerId") Long playerId, @Param("channelIds") Set<Long> channelIds);

    @Query(
            """
                SELECT m
                FROM ChatMessage m
                WHERE m.channel.id = :channelId AND (:cursor IS NULL OR m.id < :cursor)
                ORDER BY m.id DESC
            """
    )
    List<ChatMessage> findMessages(
            @Param("channelId") Long channelId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
