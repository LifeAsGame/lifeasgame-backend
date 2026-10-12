package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.domain.ChannelParticipant;
import online.lifeasgame.social.domain.ChatChannelType;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.ChannelParticipantRepository;
import online.lifeasgame.social.domain.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatReadMarker {
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final ChatReader chatReader;
    private final ChatMessageRepository chatMessageRepository;
    private final ChannelParticipantRepository channelParticipantRepository;
    private final ChatRealtimeGateway chatRealtimeGateway;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReadState mark(Long channelId, Long messageId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        if (chatReader.get(channelId).getType() != ChatChannelType.FRIEND) {
            throw new DomainException(SocialError.CHAT_READ_FRIEND_ONLY);
        }
        ChannelParticipant participant = channelParticipantRepository.findByChannelIdAndUserIdForUpdate(channelId, playerId)
                .orElseThrow(() -> new DomainException(SocialError.CHAT_CHANNEL_FORBIDDEN));
        if (messageId == null || !chatMessageRepository.existsByChannelIdAndId(channelId, messageId)) {
            throw new DomainException(SocialError.CHAT_READ_TARGET_INVALID);
        }
        boolean advanced = participant.advanceRead(messageId);
        Long position = participant.getLastReadMessageId();
        if (advanced) {
            ChatReadRealtimePayload payload = new ChatReadRealtimePayload(channelId, playerId, position);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { chatRealtimeGateway.publishRead(payload); }
                    catch (RuntimeException ex) { log.warn("Chat read realtime publish failed for channelId={}", channelId); }
                }
            });
        }
        return new ReadState(channelId, playerId, position,
                chatMessageRepository.countUnread(channelId, playerId, position));
    }

    public record ReadState(Long channelId, Long playerId, Long lastReadMessageId, long unreadCount) {}
}
