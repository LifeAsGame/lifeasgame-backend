package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.command.ChatCommand;
import online.lifeasgame.social.application.result.ChatResult;
import online.lifeasgame.social.domain.ChatChannel;
import online.lifeasgame.social.domain.ChatChannelType;
import online.lifeasgame.social.domain.ChatMessage;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.ChatMessageRepository;
import online.lifeasgame.social.domain.repository.ChatChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageRegistrar {
    private final ChatReader chatReader;
    private final ChatWriter chatWriter;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatChannelRepository chatChannelRepository;
    private final DirectChatBlockGuard directChatBlockGuard;

    @Transactional
    public ChatResult.Message register(Long playerId, Long channelId, ChatCommand.SendMessage command) {
        ChatChannel channel = chatReader.getMemberChannel(channelId, playerId);
        channel.ensureWritable();
        if (channel.getType() == ChatChannelType.FRIEND) {
            directChatBlockGuard.requireUnblocked(playerId, chatReader.getFriendPeerId(channelId, playerId));
        }
        // ponytail: per-channel lock keeps message ID and commit order aligned; revisit if hot channels need more throughput.
        chatChannelRepository.findByIdForUpdate(channelId)
                .orElseThrow(() -> new DomainException(SocialError.CHAT_CHANNEL_NOT_FOUND));
        if (command.clientMessageId() != null) {
            ChatMessage old = chatMessageRepository.findByClientMessageId(channelId, playerId, command.clientMessageId())
                    .orElse(null);
            if (old != null) {
                if (!old.getContent().equals(command.content())) {
                    throw new DomainException(SocialError.CHAT_MESSAGE_KEY_CONFLICT);
                }
                return ChatResult.Message.from(old);
            }
        }
        return ChatResult.Message.from(chatWriter.publish(channel, playerId, command.content(), command.clientMessageId()));
    }
}
