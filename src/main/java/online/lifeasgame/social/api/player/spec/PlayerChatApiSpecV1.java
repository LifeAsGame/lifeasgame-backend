package online.lifeasgame.social.api.player.spec;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.request.PlayerChatRequest;
import online.lifeasgame.social.api.player.response.PlayerChatResponse;
import org.springframework.http.ResponseEntity;

public interface PlayerChatApiSpecV1 {

    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openGlobal(@Valid PlayerChatRequest.OpenGlobal request);

    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openGuild(Long guildId);

    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openParty(Long partyId);

    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openFriend(
            Long friendId,
            @Valid PlayerChatRequest.OpenFriend request
    );

    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openAdmin(@Valid PlayerChatRequest.OpenAdmin request);

    ResponseEntity<ApiResponse<PlayerChatResponse.ChannelGroup>> myChannels();

    ResponseEntity<ApiResponse<PlayerChatResponse.MessagePage>> messages(
            Long channelId,
            Long cursor,
            @Min(1) @Max(100) int size
    );

    ResponseEntity<ApiResponse<PlayerChatResponse.Message>> sendMessage(
            Long channelId,
            @Valid PlayerChatRequest.SendMessage request
    );
}
