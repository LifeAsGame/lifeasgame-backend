package online.lifeasgame.social.infra;

import online.lifeasgame.social.domain.ChannelParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ChannelParticipantJpaRepository extends JpaRepository<ChannelParticipant, Long> {

    Optional<ChannelParticipant> findByChannelIdAndUserId(Long channelId, Long userId);

    @Query(
            """
                    select cp
                    from ChannelParticipant cp
                    join fetch cp.channel c
                    where cp.userId = :userId
                    order by c.type, c.id
            """
    )
    List<ChannelParticipant> findAllWithChannelByUserId(@Param("userId") Long userId);

    @Query(
            """
                    select cp
                    from ChannelParticipant cp
                    join fetch cp.channel c
                    where c.id in :channelIds
                    order by c.id, cp.id
            """
    )
    List<ChannelParticipant> findAllWithChannelByChannelIds(
            @Param("channelIds") Set<Long> channelIds
    );

    boolean existsByChannelId(Long channelId);
}
