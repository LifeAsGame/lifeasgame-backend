package online.lifeasgame.social.infra;

import online.lifeasgame.social.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {

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
