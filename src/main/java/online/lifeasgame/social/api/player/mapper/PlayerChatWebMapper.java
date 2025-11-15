package online.lifeasgame.social.api.player.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.social.api.player.request.PlayerChatRequest;
import online.lifeasgame.social.api.player.request.PlayerChatSocketRequest;
import online.lifeasgame.social.api.player.response.PlayerChatResponse;
import online.lifeasgame.social.application.command.ChatCommand;
import online.lifeasgame.social.application.result.ChatResult;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerChatWebMapper {

    public static ChatCommand.OpenGlobal toCommand(PlayerChatRequest.OpenGlobal request) {
        return request == null ? new ChatCommand.OpenGlobal(null) : new ChatCommand.OpenGlobal(request.name());
    }

    public static ChatCommand.OpenFriend toCommand(PlayerChatRequest.OpenFriend request) {
        return request == null ? new ChatCommand.OpenFriend(null) : new ChatCommand.OpenFriend(request.name());
    }

    public static ChatCommand.OpenAdmin toCommand(PlayerChatRequest.OpenAdmin request) {
        return request == null ? new ChatCommand.OpenAdmin(null) : new ChatCommand.OpenAdmin(request.name());
    }

    public static ChatCommand.SendMessage toCommand(PlayerChatRequest.SendMessage request) {
        return new ChatCommand.SendMessage(request.content());
    }

    public static ChatCommand.SendMessage toCommand(PlayerChatSocketRequest.SendMessage request) {
        return new ChatCommand.SendMessage(request.content());
    }

    public static PlayerChatResponse.Channel toChannel(ChatResult.Channel channel) {
        return PlayerChatResponse.Channel.from(channel);
    }

    public static PlayerChatResponse.ChannelGroup toChannelGroup(ChatResult.ChannelGroup group) {
        return PlayerChatResponse.ChannelGroup.from(group);
    }

    public static PlayerChatResponse.Message toMessage(ChatResult.Message message) {
        return PlayerChatResponse.Message.from(message);
    }

    public static PlayerChatResponse.MessagePage toMessagePage(ChatResult.MessagePage page) {
        return PlayerChatResponse.MessagePage.from(page);
    }
}
