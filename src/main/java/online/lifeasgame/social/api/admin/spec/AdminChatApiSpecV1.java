package online.lifeasgame.social.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.admin.request.AdminChatRequest;
import online.lifeasgame.social.api.admin.response.AdminChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Social Chat API V1 (Admin)")
public interface AdminChatApiSpecV1 {

    @Operation(summary = "운영자용 채널 개설")
    ResponseEntity<ApiResponse<AdminChatResponse.Channel>> openAdmin(
            @PathVariable Long operatorId,
            @PathVariable Long playerId,
            @Valid @RequestBody AdminChatRequest.OpenAdmin request
    );

    @Operation(summary = "운영자 채팅 채널 목록")
    ResponseEntity<ApiResponse<AdminChatResponse.ChannelGroup>> channels(@PathVariable Long operatorId);

    @Operation(summary = "운영자 채팅 메시지 조회")
    ResponseEntity<ApiResponse<AdminChatResponse.MessagePage>> messages(
            @PathVariable Long operatorId,
            @PathVariable Long channelId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size
    );

    @Operation(summary = "운영자 채팅 메시지 전송")
    ResponseEntity<ApiResponse<AdminChatResponse.Message>> sendMessage(
            @PathVariable Long operatorId,
            @PathVariable Long channelId,
            @Valid @RequestBody AdminChatRequest.SendMessage request
    );
}
