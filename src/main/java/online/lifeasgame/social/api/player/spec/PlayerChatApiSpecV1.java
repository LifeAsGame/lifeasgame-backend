package online.lifeasgame.social.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.request.PlayerChatRequest;
import online.lifeasgame.social.api.player.response.PlayerChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Social Chat API V1 (Player)")
public interface PlayerChatApiSpecV1 {

    @Operation(summary = "글로벌 채널 열기")
    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openGlobal(
            @Valid @RequestBody PlayerChatRequest.OpenGlobal request
    );

    @Operation(summary = "길드 채널 열기")
    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openGuild(
            @PathVariable Long guildId
    );

    @Operation(summary = "파티 채널 열기")
    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openParty(
            @PathVariable Long partyId
    );

    @Operation(summary = "친구 채널 열기(1:1)")
    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openFriend(
            @PathVariable Long targetPlayerId,
            @Valid @RequestBody PlayerChatRequest.OpenFriend request
    );

    @Operation(summary = "운영자 채널 열기")
    ResponseEntity<ApiResponse<PlayerChatResponse.Channel>> openAdmin(
            @Valid @RequestBody PlayerChatRequest.OpenAdmin request
    );

    @Operation(summary = "내 채널 목록")
    ResponseEntity<ApiResponse<PlayerChatResponse.ChannelGroup>> myChannels();

    @Operation(summary = "친구 채널 목록")
    ResponseEntity<ApiResponse<List<PlayerChatResponse.FriendChannel>>> friendChannels();

    @Operation(summary = "채널 메시지 조회")
    ResponseEntity<ApiResponse<PlayerChatResponse.MessagePage>> messages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size
    );

    @Operation(summary = "메시지 전송(HTTP)")
    ResponseEntity<ApiResponse<PlayerChatResponse.Message>> sendMessage(
            @PathVariable Long channelId,
            @Valid @RequestBody PlayerChatRequest.SendMessage request
    );
}
