package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.result.ChatResult;
import online.lifeasgame.social.domain.ChannelParticipant;
import online.lifeasgame.social.domain.ChatChannel;
import online.lifeasgame.social.domain.ChatChannelType;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.ChannelParticipantRepository;
import online.lifeasgame.social.domain.repository.ChatChannelRepository;
import online.lifeasgame.social.domain.repository.ChatMessageRepository;
import online.lifeasgame.social.domain.repository.ChatMessageRepository.MessageSlice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ChatReader {

    private final ChatChannelRepository chatChannelRepository;
    private final ChannelParticipantRepository channelParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatChannel get(Long channelId) {
        return chatChannelRepository.findById(channelId).orElseThrow(() -> new DomainException(SocialError.CHAT_CHANNEL_NOT_FOUND));
    }

    public ChatChannel getMemberChannel(Long channelId, Long playerId) {
        ChatChannel channel = get(channelId);
        channelParticipantRepository.findByChannelIdAndUserId(
                channelId,
                playerId
        ).orElseThrow(() -> new DomainException(SocialError.CHAT_CHANNEL_FORBIDDEN));
        return channel;
    }

    public Optional<ChannelParticipant> findParticipant(Long channelId, Long playerId) {
        return channelParticipantRepository.findByChannelIdAndUserId(channelId, playerId);
    }

    public ChatResult.ChannelGroup list(Long playerId) {
        List<ChannelParticipant> participants = channelParticipantRepository.findAllByUserId(playerId);
        Map<ChatChannelType, List<ChatResult.Channel>> grouped = new EnumMap<>(ChatChannelType.class);

        for (ChannelParticipant participant : participants) {
            ChatChannelType type = participant.getChannel().getType();
            grouped.computeIfAbsent(type, key -> new ArrayList<>()).add(ChatResult.Channel.from(participant));
        }

        return ChatResult.ChannelGroup.of(grouped);
    }

    public ChatResult.MessagePage messages(Long channelId, Long playerId, Long cursor, int size) {
        getMemberChannel(channelId, playerId);
        MessageSlice slice = chatMessageRepository.fetchMessages(channelId, cursor, size);
        List<ChatResult.Message> messages = slice.messages().stream().map(ChatResult.Message::from).toList();
        return ChatResult.MessagePage.from(messages, slice.hasMore(), slice.nextCursor());
    }
}
