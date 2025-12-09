package online.lifeasgame.social.api.player.mapper;

import online.lifeasgame.social.api.player.request.PlayerChatRequest;
import online.lifeasgame.social.api.player.request.PlayerChatSocketRequest;
import online.lifeasgame.social.api.player.response.PlayerChatResponse;
import online.lifeasgame.social.application.command.ChatCommand;
import online.lifeasgame.social.application.result.ChatResult;

public final class PlayerChatWebMapper {

    private PlayerChatWebMapper() {}

    public static PlayerChatResponse.ChannelGroup toChannelGroup(ChatResult.ChannelGroup result) {
        return new PlayerChatResponse.ChannelGroup(
                result.global().stream().map(PlayerChatWebMapper::toChannel).toList(),
                result.guild().stream().map(PlayerChatWebMapper::toChannel).toList(),
                result.party().stream().map(PlayerChatWebMapper::toChannel).toList(),
                result.admin().stream().map(PlayerChatWebMapper::toChannel).toList(),
                result.friend().stream().map(PlayerChatWebMapper::toChannel).toList()
        );
    }

    public static ChatCommand.OpenAdmin toOpenAdminCommand(PlayerChatRequest.OpenAdmin request) {
        return request == null ?
                new ChatCommand.OpenAdmin(null) : new ChatCommand.OpenAdmin(request.name());
    }

    public static ChatCommand.OpenGlobal toOpenGlobalCommand(PlayerChatRequest.OpenGlobal request) {
        return request == null ?
                new ChatCommand.OpenGlobal(null) : new ChatCommand.OpenGlobal(request.name());
    }

    public static ChatCommand.OpenFriend toOpenFriendCommand(PlayerChatRequest.OpenFriend request) {
        return request == null ?
                new ChatCommand.OpenFriend(null) : new ChatCommand.OpenFriend(request.name());
    }

    public static PlayerChatResponse.Channel toChannel(ChatResult.Channel result) {
        return new PlayerChatResponse.Channel(
                result.id(),
                result.type(),
                result.name(),
                result.contextId(),
                result.readOnly(),
                result.role()
        );
    }

    public static PlayerChatResponse.MessagePage toMessagePage(ChatResult.MessagePage result) {
        return new PlayerChatResponse.MessagePage(
                result.messages().stream()
                        .map(PlayerChatWebMapper::toMessage)
                        .toList(),
                result.hasMore(),
                result.nextCursor()
        );
    }

    public static ChatCommand.SendMessage toSendMessageCommand(PlayerChatRequest.SendMessage request) {
        return new ChatCommand.SendMessage(request.content());
    }

    public static PlayerChatResponse.Message toMessage(ChatResult.Message result) {
        return new PlayerChatResponse.Message(
                result.id(),
                result.channelId(),
                result.senderId(),
                result.content(),
                result.edited(),
                result.createdAt()
        );
    }

    public static ChatCommand.SendMessage toSendMessageCommand(PlayerChatSocketRequest.SendMessage request) {
        return new ChatCommand.SendMessage(request.content());
    }
}
