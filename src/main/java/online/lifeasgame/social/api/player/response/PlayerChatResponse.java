package online.lifeasgame.social.api.player.response;

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
    }

    public record ChannelGroup(
            List<Channel> global,
            List<Channel> guild,
            List<Channel> party,
            List<Channel> admin,
            List<Channel> friend
    ) {
    }

    public record Message(
            Long id,
            Long channelId,
            Long senderId,
            String content,
            boolean edited,
            Instant createdAt
    ) {
    }

    public record MessagePage(
            List<Message> messages,
            boolean hasMore,
            Long nextCursor
    ) {
    }
}
