package online.lifeasgame.social.api.player.response;

import online.lifeasgame.social.application.result.ChatResult;
import online.lifeasgame.social.domain.ChannelRole;
import online.lifeasgame.social.domain.ChatChannelType;

import java.time.Instant;
import java.util.List;

public final class PlayerChatResponse {

    private PlayerChatResponse() {
    }

    public record Channel(
            Long id,
            ChatChannelType type,
            String name,
            Long contextId,
            boolean readOnly,
            ChannelRole role
    ) {
        public static Channel from(ChatResult.Channel channel) {
            return new Channel(
                    channel.id(),
                    channel.type(),
                    channel.name(),
                    channel.contextId(),
                    channel.readOnly(),
                    channel.role()
            );
        }
    }

    public record ChannelGroup(
            List<Channel> global,
            List<Channel> guild,
            List<Channel> party,
            List<Channel> admin,
            List<Channel> friend
    ) {
        public static ChannelGroup from(ChatResult.ChannelGroup group) {
            return new ChannelGroup(
                    group.global().stream().map(Channel::from).toList(),
                    group.guild().stream().map(Channel::from).toList(),
                    group.party().stream().map(Channel::from).toList(),
                    group.admin().stream().map(Channel::from).toList(),
                    group.friend().stream().map(Channel::from).toList()
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
        public static Message from(ChatResult.Message message) {
            return new Message(
                    message.id(),
                    message.channelId(),
                    message.senderId(),
                    message.content(),
                    message.edited(),
                    message.createdAt()
            );
        }
    }

    public record MessagePage(
            List<Message> messages,
            boolean hasMore,
            Long nextCursor
    ) {
        public static MessagePage from(ChatResult.MessagePage page) {
            return new MessagePage(
                    page.messages().stream().map(Message::from).toList(),
                    page.hasMore(),
                    page.nextCursor()
            );
        }
    }
}
