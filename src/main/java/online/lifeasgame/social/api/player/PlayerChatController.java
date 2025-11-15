package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.player.mapper.PlayerChatWebMapper;
import online.lifeasgame.social.api.player.request.PlayerChatRequest;
import online.lifeasgame.social.api.player.response.PlayerChatResponse;
import online.lifeasgame.social.api.player.spec.PlayerChatApiSpecV1;
import online.lifeasgame.social.application.ChatFacade;
import online.lifeasgame.social.application.result.ChatResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class PlayerChatController implements PlayerChatApiSpecV1 {

    private final ChatFacade chatFacade;

    @Override
    @PostMapping("/channels/global")
    public ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openGlobal(
            @Valid @RequestBody PlayerChatRequest.OpenGlobal request
    ) {
        ChatResult.Channel channel = chatFacade.openGlobal(PlayerChatWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerChatWebMapper.toChannel(channel));
    }

    @Override
    @PostMapping("/channels/guild/{guildId}")
    public ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openGuild(@PathVariable Long guildId) {
        ChatResult.Channel channel = chatFacade.openGuild(guildId);
        return ApiResponses.ok(PlayerChatWebMapper.toChannel(channel));
    }

    @Override
    @PostMapping("/channels/party/{partyId}")
    public ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openParty(@PathVariable Long partyId) {
        ChatResult.Channel channel = chatFacade.openParty(partyId);
        return ApiResponses.ok(PlayerChatWebMapper.toChannel(channel));
    }

    @Override
    @PostMapping("/channels/friend/{friendId}")
    public ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openFriend(
            @PathVariable Long friendId,
            @Valid @RequestBody PlayerChatRequest.OpenFriend request
    ) {
        ChatResult.Channel channel = chatFacade.openFriend(friendId, PlayerChatWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerChatWebMapper.toChannel(channel));
    }

    @Override
    @PostMapping("/channels/admin")
    public ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openAdmin(
            @Valid @RequestBody PlayerChatRequest.OpenAdmin request
    ) {
        ChatResult.Channel channel = chatFacade.openAdmin(PlayerChatWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerChatWebMapper.toChannel(channel));
    }

    @Override
    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<PlayerChatResponse.ChannelGroup>> myChannels() {
        ChatResult.ChannelGroup group = chatFacade.myChannels();
        return ApiResponses.ok(PlayerChatWebMapper.toChannelGroup(group));
    }

    @Override
    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<PlayerChatResponse.MessagePage>> messages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        ChatResult.MessagePage page = chatFacade.messages(channelId, cursor, size);
        return ApiResponses.ok(PlayerChatWebMapper.toMessagePage(page));
    }

    @Override
    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<PlayerChatResponse.Message>> sendMessage(
            @PathVariable Long channelId,
            @Valid @RequestBody PlayerChatRequest.SendMessage request
    ) {
        ChatResult.Message message = chatFacade.sendMessage(channelId, PlayerChatWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerChatWebMapper.toMessage(message));
    }
}
