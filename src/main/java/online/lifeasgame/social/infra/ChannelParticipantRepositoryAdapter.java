package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.ChannelParticipant;
import online.lifeasgame.social.domain.repository.ChannelParticipantRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChannelParticipantRepositoryAdapter implements ChannelParticipantRepository {

    private final ChannelParticipantJpaRepository channelParticipantJpaRepository;

    @Override
    public ChannelParticipant save(ChannelParticipant participant) {
        return channelParticipantJpaRepository.save(participant);
    }

    @Override
    public Optional<ChannelParticipant> findByChannelIdAndUserId(Long channelId, Long userId) {
        return channelParticipantJpaRepository.findByChannelIdAndUserId(channelId, userId);
    }

    @Override
    public List<ChannelParticipant> findAllByUserId(Long userId) {
        return channelParticipantJpaRepository.findAllWithChannelByUserId(userId);
    }

    @Override
    public boolean existsByChannelId(Long channelId) {
        return channelParticipantJpaRepository.existsByChannelId(channelId);
    }
}
