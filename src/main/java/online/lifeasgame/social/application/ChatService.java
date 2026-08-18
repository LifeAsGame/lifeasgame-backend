package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.command.ChatCommand;
import online.lifeasgame.social.application.model.ChatSpec;
import online.lifeasgame.social.application.result.ChatResult;
import online.lifeasgame.social.domain.*;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatReader chatReader;
    private final ChatWriter chatWriter;
    private final GuildReader guildReader;
    private final PartyReader partyReader;
    private final FriendshipVerifier friendshipVerifier;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public ChatResult.Channel openGlobal(ChatCommand.OpenGlobal command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        ChatSpec.OpenGlobal spec = ChatSpec.OpenGlobal.from(command);
        String channelName = (spec.name() == null || spec.name().isBlank()) ? "Global" : spec.name();
        ChatChannel channel = chatWriter.ensureGlobalChannel(channelName);
        ChannelParticipant participant = chatWriter.join(channel, playerId, null);
        return ChatResult.Channel.from(participant);
    }

    @Transactional
    public ChatResult.Channel openAdmin(ChatCommand.OpenAdmin command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        ChatSpec.OpenAdmin spec = ChatSpec.OpenAdmin.forPlayer(playerId, command);
        ChatChannel channel = chatWriter.ensureAdminChannel(spec.contextId(), spec.name());
        ChannelParticipant participant = chatWriter.join(channel, playerId, null);
        return ChatResult.Channel.from(participant);
    }

    @Transactional
    public ChatResult.Channel openAdminForOperator(Long operatorId, Long playerId, ChatCommand.OpenAdmin command) {
        // 운영진이 플레이어를 위해 운영진 채널을 여는 경우 검증 필요
        ChatSpec.OpenAdmin spec = ChatSpec.OpenAdmin.forOperator(operatorId, playerId, command);
        ChatChannel channel = chatWriter.ensureAdminChannel(spec.contextId(), spec.name());
        chatWriter.join(channel, playerId, null);
        ChannelParticipant participant = chatWriter.join(channel, operatorId, ChannelRole.ADMIN);
        return ChatResult.Channel.from(participant);
    }

    @Transactional
    public ChatResult.Channel openGuild(Long guildId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Guild guild = guildReader.getByIdOrThrow(guildId);
        guild.findMember(playerId).orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        ChatChannel channel = chatWriter.ensureGuildChannel(guildId, guild.getName().getOriginal());
        ChannelParticipant participant = chatWriter.join(channel, playerId, null);
        return ChatResult.Channel.from(participant);
    }

    @Transactional
    public ChatResult.Channel openParty(Long partyId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Party party = partyReader.getById(partyId);
        party.findMember(playerId).orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        ChatChannel channel = chatWriter.ensurePartyChannel(partyId, party.getName().getOriginal());
        ChannelParticipant participant = chatWriter.join(channel, playerId, null);
        return ChatResult.Channel.from(participant);
    }

    @Transactional
    public ChatResult.Channel openFriend(Long friendId, ChatCommand.OpenFriend command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        friendshipVerifier.verify(playerId, friendId);

        ChatSpec.OpenFriend spec = ChatSpec.OpenFriend.from(playerId, friendId, command);
        ChatChannel channel = chatWriter.ensureFriendChannel(spec.playerId(), spec.friendId(), spec.name());
        ChannelParticipant participant = chatWriter.join(channel, playerId, null);
        chatWriter.join(channel, friendId, null);
        return ChatResult.Channel.from(participant);
    }

    @Transactional(readOnly = true)
    public ChatResult.ChannelGroup myChannels() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return chatReader.list(playerId);
    }

    @Transactional(readOnly = true)
    public ChatResult.ChannelGroup myChannels(Long playerId) {
        return chatReader.list(playerId);
    }

    @Transactional(readOnly = true)
    public ChatResult.MessagePage messages(Long channelId, Long cursor, int size) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return chatReader.messages(channelId, playerId, cursor, size);
    }

    @Transactional(readOnly = true)
    public ChatResult.MessagePage messages(Long playerId, Long channelId, Long cursor, int size) {
        return chatReader.messages(channelId, playerId, cursor, size);
    }

    @Transactional
    public ChatResult.Message sendMessage(Long channelId, ChatCommand.SendMessage command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return sendMessageFor(playerId, channelId, command);
    }

    @Transactional
    public ChatResult.Message sendMessage(Long playerId, Long channelId, ChatCommand.SendMessage command) {
        return sendMessageFor(playerId, channelId, command);
    }

    private ChatResult.Message sendMessageFor(
            Long playerId,
            Long channelId,
            ChatCommand.SendMessage command
    ) {
        ChatSpec.SendMessage spec = ChatSpec.SendMessage.from(channelId, playerId, command);
        ChatChannel channel = chatReader.getMemberChannel(spec.channelId(), spec.senderId());
        channel.ensureWritable();
        ChatMessage saved = chatWriter.publish(channel, playerId, spec.content());
        return ChatResult.Message.from(saved);
    }
}
