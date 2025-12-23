package online.lifeasgame.social.api.admin.mapper;

import online.lifeasgame.social.api.admin.request.AdminChatRequest;
import online.lifeasgame.social.api.admin.response.AdminChatResponse;
import online.lifeasgame.social.application.command.ChatCommand;
import online.lifeasgame.social.application.result.ChatResult;

public final class AdminChatWebMapper {

    private AdminChatWebMapper() {
    }

    public static AdminChatResponse.ChannelGroup toChannelGroup(ChatResult.ChannelGroup result) {
        return new AdminChatResponse.ChannelGroup(
                result.global().stream().map(AdminChatWebMapper::toChannel).toList(),
                result.guild().stream().map(AdminChatWebMapper::toChannel).toList(),
                result.party().stream().map(AdminChatWebMapper::toChannel).toList(),
                result.admin().stream().map(AdminChatWebMapper::toChannel).toList(),
                result.friend().stream().map(AdminChatWebMapper::toChannel).toList()
        );
    }

    public static ChatCommand.OpenAdmin toOpenAdminCommand(AdminChatRequest.OpenAdmin request) {
        return request == null ?
                new ChatCommand.OpenAdmin(null) : new ChatCommand.OpenAdmin(request.name());
    }

    public static AdminChatResponse.Channel toChannel(ChatResult.Channel result) {
        return new AdminChatResponse.Channel(
                result.id(),
                result.type(),
                result.name(),
                result.contextId(),
                result.readOnly(),
                result.role()
        );
    }

    public static AdminChatResponse.MessagePage toMessagePage(ChatResult.MessagePage result) {
        return new AdminChatResponse.MessagePage(
                result.messages().stream()
                        .map(AdminChatWebMapper::toMessage)
                        .toList(),
                result.hasMore(),
                result.nextCursor()
        );
    }

    public static ChatCommand.SendMessage toSendMessageCommand(AdminChatRequest.SendMessage request) {
        return new ChatCommand.SendMessage(request.content());
    }

    public static AdminChatResponse.Message toMessage(ChatResult.Message result) {
        return new AdminChatResponse.Message(
                result.id(),
                result.channelId(),
                result.senderId(),
                result.content(),
                result.edited(),
                result.createdAt()
        );
    }
}
