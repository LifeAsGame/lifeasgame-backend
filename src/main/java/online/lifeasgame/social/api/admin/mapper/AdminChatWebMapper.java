package online.lifeasgame.social.api.admin.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.social.api.admin.request.AdminChatRequest;
import online.lifeasgame.social.api.admin.response.AdminChatResponse;
import online.lifeasgame.social.application.command.ChatCommand;
import online.lifeasgame.social.application.result.ChatResult;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AdminChatWebMapper {

    public static ChatCommand.OpenAdmin toCommand(AdminChatRequest.OpenAdmin request) {
        return request == null ? new ChatCommand.OpenAdmin(null) : new ChatCommand.OpenAdmin(request.name());
    }

    public static ChatCommand.SendMessage toCommand(AdminChatRequest.SendMessage request) {
        return new ChatCommand.SendMessage(request.content());
    }

    public static AdminChatResponse.Channel toChannel(ChatResult.Channel channel) {
        return AdminChatResponse.Channel.from(channel);
    }

    public static AdminChatResponse.ChannelGroup toChannelGroup(ChatResult.ChannelGroup group) {
        return AdminChatResponse.ChannelGroup.from(group);
    }

    public static AdminChatResponse.Message toMessage(ChatResult.Message message) {
        return AdminChatResponse.Message.from(message);
    }

    public static AdminChatResponse.MessagePage toMessagePage(ChatResult.MessagePage page) {
        return AdminChatResponse.MessagePage.from(page);
    }
}
