package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.command.ChatCommand;
import online.lifeasgame.social.application.result.ChatResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatFacade {

    private final ChatService chatService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    private Long player() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }

    public ChatResult.Channel openGlobal(ChatCommand.OpenGlobal command) {
        return chatService.openGlobal(player(), command);
    }

    public ChatResult.Channel openGuild(Long guildId) {
        return chatService.openGuild(player(), guildId);
    }

    public ChatResult.Channel openParty(Long partyId) {
        return chatService.openParty(player(), partyId);
    }

    public ChatResult.Channel openFriend(Long friendId, ChatCommand.OpenFriend command) {
        return chatService.openFriend(player(), friendId, command);
    }

    public ChatResult.Channel openAdmin(ChatCommand.OpenAdmin command) {
        return chatService.openAdmin(player(), command);
    }

    public ChatResult.ChannelGroup myChannels() {
        return chatService.myChannels(player());
    }

    public ChatResult.MessagePage messages(Long channelId, Long cursor, int size) {
        return chatService.messages(player(), channelId, cursor, size);
    }

    public ChatResult.Message sendMessage(Long channelId, ChatCommand.SendMessage command) {
        return chatService.sendMessage(player(), channelId, command);
    }
}
