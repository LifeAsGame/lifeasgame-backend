package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.ChannelParticipant;

import java.util.List;
import java.util.Optional;

public interface ChannelParticipantRepository {

    ChannelParticipant save(ChannelParticipant participant);

    Optional<ChannelParticipant> findByChannelIdAndUserId(Long channelId, Long userId);

    List<ChannelParticipant> findAllByUserId(Long userId);

    boolean existsByChannelId(Long channelId);
}

