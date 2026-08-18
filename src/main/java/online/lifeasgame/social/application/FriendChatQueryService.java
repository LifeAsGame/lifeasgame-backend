package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerConnectionReadApi;
import online.lifeasgame.character.application.internal.PlayerConnectionReadApi.PlayerSummary;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.result.ChatResult;
import online.lifeasgame.social.domain.ChannelParticipant;
import online.lifeasgame.social.domain.ChatChannel;
import online.lifeasgame.social.domain.ChatChannelType;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.ChannelParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class FriendChatQueryService {

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final ChannelParticipantRepository channelParticipantRepository;
    private final PlayerConnectionReadApi playerConnectionReadApi;

    public List<ChatResult.FriendChannel> friendChannels() {
        Long currentPlayerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<ChatChannel> channels = channelParticipantRepository.findAllByUserId(currentPlayerId)
                .stream()
                .map(ChannelParticipant::getChannel)
                .filter(channel -> channel.getType() == ChatChannelType.FRIEND)
                .sorted(Comparator.comparing(ChatChannel::getId))
                .toList();

        if (channels.isEmpty()) {
            return List.of();
        }

        Set<Long> channelIds = channels.stream()
                .map(ChatChannel::getId)
                .collect(Collectors.toSet());
        Map<Long, List<Long>> participantIdsByChannel = channelParticipantRepository
                .findAllByChannelIds(channelIds)
                .stream()
                .collect(Collectors.groupingBy(
                        participant -> participant.getChannel().getId(),
                        Collectors.mapping(ChannelParticipant::getUserId, Collectors.toList())
                ));

        Map<Long, Long> peerIdByChannel = new HashMap<>();
        for (ChatChannel channel : channels) {
            List<Long> peerIds = participantIdsByChannel.getOrDefault(channel.getId(), List.of())
                    .stream()
                    .filter(participantId -> !participantId.equals(currentPlayerId))
                    .toList();
            if (peerIds.size() != 1) {
                throw new DomainException(SocialError.CHAT_FRIEND_PARTICIPANT_INVALID);
            }
            peerIdByChannel.put(channel.getId(), peerIds.getFirst());
        }

        Set<Long> peerIds = Set.copyOf(peerIdByChannel.values());
        Map<Long, PlayerSummary> peers = playerConnectionReadApi.findAllByPlayerIds(peerIds);
        return channels.stream()
                .map(channel -> {
                    PlayerSummary peer = peers.get(peerIdByChannel.get(channel.getId()));
                    if (peer == null) {
                        throw new DomainException(SocialError.CHAT_PEER_NOT_FOUND);
                    }
                    return new ChatResult.FriendChannel(
                            channel.getId(),
                            new ChatResult.Peer(peer.playerId(), peer.name(), peer.job(), peer.level()),
                            channel.isReadOnly()
                    );
                })
                .toList();
    }
}
