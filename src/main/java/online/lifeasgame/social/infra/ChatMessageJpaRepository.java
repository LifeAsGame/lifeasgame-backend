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
                select m
                from ChatMessage m
                where m.channel.id = :channelId and (:cursor is null or m.id < :cursor)
                order by m.id desc
            """
    )
    List<ChatMessage> findMessages(
            @Param("channelId") Long channelId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
