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
import online.lifeasgame.social.domain.repository.ChatMessageRepository;
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
    private final ChatMessageRepository chatMessageRepository;

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
        Map<Long, List<ChannelParticipant>> participantsByChannel = channelParticipantRepository
                .findAllByChannelIds(channelIds)
                .stream()
                .collect(Collectors.groupingBy(
                        participant -> participant.getChannel().getId()
                ));

        Map<Long, Long> peerIdByChannel = new HashMap<>();
        for (ChatChannel channel : channels) {
            List<Long> peerIds = participantsByChannel.getOrDefault(channel.getId(), List.of())
                    .stream()
                    .map(ChannelParticipant::getUserId)
                    .filter(participantId -> !participantId.equals(currentPlayerId))
                    .toList();
            if (peerIds.size() != 1) {
                throw new DomainException(SocialError.CHAT_FRIEND_PARTICIPANT_INVALID);
            }
            peerIdByChannel.put(channel.getId(), peerIds.getFirst());
        }

        Set<Long> peerIds = Set.copyOf(peerIdByChannel.values());
        Map<Long, PlayerSummary> peers = playerConnectionReadApi.findAllByPlayerIds(peerIds);
        Map<Long, Long> unread = chatMessageRepository.unreadCounts(currentPlayerId, channelIds);
        return channels.stream()
                .map(channel -> {
                    PlayerSummary peer = peers.get(peerIdByChannel.get(channel.getId()));
                    if (peer == null) {
                        throw new DomainException(SocialError.CHAT_PEER_NOT_FOUND);
                    }
                    List<ChannelParticipant> participants = participantsByChannel.get(channel.getId());
                    Long mine = participants.stream().filter(p -> p.getUserId().equals(currentPlayerId))
                            .findFirst().orElseThrow().getLastReadMessageId();
                    Long theirs = participants.stream().filter(p -> p.getUserId().equals(peer.playerId()))
                            .findFirst().orElseThrow().getLastReadMessageId();
                    return new ChatResult.FriendChannel(
                            channel.getId(),
                            new ChatResult.Peer(peer.playerId(), peer.name(), peer.job(), peer.level()),
                            channel.isReadOnly(),
                            mine,
                            theirs,
                            unread.getOrDefault(channel.getId(), 0L)
                    );
                })
                .toList();
    }
}
