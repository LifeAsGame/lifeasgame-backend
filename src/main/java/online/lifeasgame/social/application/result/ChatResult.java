package online.lifeasgame.social.application.result;

import online.lifeasgame.social.domain.*;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ChatResult {

    private ChatResult() {
    }

    public record Channel(
            Long id,
            ChatChannelType type,
            String name,
            Long contextId,
            boolean readOnly,
            ChannelRole role
    ) {
        public static Channel from(ChatChannel channel, ChannelRole role) {
            return new Channel(
                    channel.getId(),
                    channel.getType(),
                    channel.getName() == null ? null : channel.getName().getValue(),
                    channel.getContextId(),
                    channel.isReadOnly(),
                    role
            );
        }

        public static Channel from(ChannelParticipant participant) {
            return from(participant.getChannel(), participant.getRole());
        }
    }

    public record ChannelGroup(
            List<Channel> global,
            List<Channel> guild,
            List<Channel> party,
            List<Channel> admin,
            List<Channel> friend
    ) {
        public static ChannelGroup of(Map<ChatChannelType, List<Channel>> grouped) {
            return new ChannelGroup(
                    List.copyOf(grouped.getOrDefault(ChatChannelType.GLOBAL, List.of())),
                    List.copyOf(grouped.getOrDefault(ChatChannelType.GUILD, List.of())),
                    List.copyOf(grouped.getOrDefault(ChatChannelType.PARTY, List.of())),
                    List.copyOf(grouped.getOrDefault(ChatChannelType.ADMIN, List.of())),
                    List.copyOf(grouped.getOrDefault(ChatChannelType.FRIEND, List.of()))
            );
        }
    }

    public record Message(
            Long id,
            Long channelId,
            Long senderId,
            String content,
            boolean edited,
            Instant createdAt
    ) {
        public static Message from(ChatMessage message) {
            return new Message(
                    message.getId(),
                    message.getChannel().getId(),
                    message.getSenderId(),
                    message.getContent(),
                    message.isEdited(),
                    message.getCreatedAt()
            );
        }
    }

    public record MessagePage(List<Message> messages, boolean hasMore, Long nextCursor) {
        public static MessagePage from(List<Message> messages, boolean hasMore, Long nextCursor) {
            return new MessagePage(Collections.unmodifiableList(messages), hasMore, nextCursor);
        }
    }

    public static Map<ChatChannelType, List<Channel>> emptyGroupedMap() {
        return new EnumMap<>(ChatChannelType.class);
    }
}
