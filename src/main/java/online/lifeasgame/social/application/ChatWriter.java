package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.social.domain.*;
import online.lifeasgame.social.domain.repository.ChannelParticipantRepository;
import online.lifeasgame.social.domain.repository.ChatChannelRepository;
import online.lifeasgame.social.domain.repository.ChatMessageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ChatWriter {

    private final ChatChannelRepository chatChannelRepository;
    private final ChannelParticipantRepository channelParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRealtimeGateway chatRealtimeGateway;

    public ChatChannel ensureGlobalChannel(String name) {
        String normalized = normalizeName(name, "Global");
        return chatChannelRepository.findByTypeAndName(
                ChatChannelType.GLOBAL,
                normalized
        ).orElseGet(
                () -> chatChannelRepository.save(ChatChannel.global(normalized))
        );
    }

    public ChatChannel ensureGuildChannel(Long guildId, String name) {
        return chatChannelRepository.findByContext(
                ChatChannelType.GUILD,
                guildId
        ).orElseGet(() -> chatChannelRepository.save(ChatChannel.guild(guildId, name)));
    }

    public ChatChannel ensurePartyChannel(Long partyId, String name) {
        return chatChannelRepository.findByContext(
                ChatChannelType.PARTY,
                partyId
        ).orElseGet(() -> chatChannelRepository.save(ChatChannel.party(partyId, name)));
    }

    public ChatChannel ensureAdminChannel(Long playerId, String name) {
        String normalized = normalizeName(name, "운영진 문의");
        return chatChannelRepository.findByContext(
                ChatChannelType.ADMIN,
                playerId
        ).orElseGet(() -> chatChannelRepository.save(ChatChannel.admin(playerId, normalized)));
    }

    public ChatChannel ensureFriendChannel(Long playerId, Long friendId, String name) {
        Guard.check(!playerId.equals(friendId), "friend channel participants must differ");
        String normalized = normalizeName(name, "친구 채팅");
        return chatChannelRepository.findFriendChannel(playerId, friendId)
                .orElseGet(() -> chatChannelRepository.save(ChatChannel.friend(normalized)));
    }

    public ChannelParticipant join(ChatChannel channel, Long userId, ChannelRole preferredRole) {
        return channelParticipantRepository.findByChannelIdAndUserId(channel.getId(), userId).orElseGet(() -> {
            ChannelRole role = preferredRole;
            if (role == null) {
                boolean exists = channelParticipantRepository.existsByChannelId(channel.getId());
                if (!exists && channel.getType() == ChatChannelType.ADMIN) {
                    role = ChannelRole.MEMBER;
                } else {
                    role = exists ? ChannelRole.MEMBER : ChannelRole.OWNER;
                }
            }
            return channelParticipantRepository.save(new ChannelParticipant(channel, userId, role));
        });
    }

    public ChatMessage publish(ChatChannel channel, Long senderId, String content) {
        channel.ensureWritable();
        ChatMessage saved = chatMessageRepository.save(ChatMessage.create(channel, senderId, content));
        chatRealtimeGateway.publish(ChatRealtimePayload.from(saved));
        return saved;
    }

    private String normalizeName(String name, String defaultName) {
        String candidate = (name == null || name.isBlank()) ? defaultName : name;
        return ChannelName.of(candidate).getValue();
    }
}
