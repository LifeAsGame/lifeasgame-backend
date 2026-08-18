package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.ChannelParticipant;
import online.lifeasgame.social.domain.repository.ChannelParticipantRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ChannelParticipantRepositoryAdapter implements ChannelParticipantRepository {

    private final ChannelParticipantJpaRepository jpaRepository;

    @Override
    public ChannelParticipant save(ChannelParticipant participant) {
        return jpaRepository.save(participant);
    }

    @Override
    public Optional<ChannelParticipant> findByChannelIdAndUserId(Long channelId, Long userId) {
        return jpaRepository.findByChannelIdAndUserId(channelId, userId);
    }

    @Override
    public List<ChannelParticipant> findAllByUserId(Long userId) {
        return jpaRepository.findAllWithChannelByUserId(userId);
    }

    @Override
    public List<ChannelParticipant> findAllByChannelIds(Set<Long> channelIds) {
        return jpaRepository.findAllWithChannelByChannelIds(channelIds);
    }

    @Override
    public boolean existsByChannelId(Long channelId) {
        return jpaRepository.existsByChannelId(channelId);
    }
}
