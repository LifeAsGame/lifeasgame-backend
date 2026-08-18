package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.ChannelParticipant;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChannelParticipantRepository {

    ChannelParticipant save(ChannelParticipant participant);

    Optional<ChannelParticipant> findByChannelIdAndUserId(Long channelId, Long userId);

    List<ChannelParticipant> findAllByUserId(Long userId);

    List<ChannelParticipant> findAllByChannelIds(Set<Long> channelIds);

    boolean existsByChannelId(Long channelId);
}
