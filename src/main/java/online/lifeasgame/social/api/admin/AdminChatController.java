package online.lifeasgame.social.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.admin.mapper.AdminChatWebMapper;
import online.lifeasgame.social.api.admin.request.AdminChatRequest;
import online.lifeasgame.social.api.admin.response.AdminChatResponse;
import online.lifeasgame.social.api.admin.spec.AdminChatApiSpecV1;
import online.lifeasgame.social.application.ChatService;
import online.lifeasgame.social.application.result.ChatResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/operators/{operatorId}/chat")
public class AdminChatController implements AdminChatApiSpecV1 {

    private final ChatService chatService;

    @Override
    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<AdminChatResponse.ChannelGroup>> channels(@PathVariable Long operatorId) {
        ChatResult.ChannelGroup result = chatService.myChannels(operatorId);
        return ApiResponses.ok(AdminChatWebMapper.toChannelGroup(result));
    }

    @Override
    @PostMapping("/channels/player/{playerId}")
    public ResponseEntity<ApiResponse<AdminChatResponse.Channel>> openAdmin(
            @PathVariable Long operatorId,
            @PathVariable Long playerId,
            @Valid @RequestBody AdminChatRequest.OpenAdmin request
    ) {
        ChatResult.Channel result = chatService.openAdminForOperator(
                operatorId,
                playerId,
                AdminChatWebMapper.toOpenAdminCommand(request)
        );

        return ApiResponses.ok(AdminChatWebMapper.toChannel(result));
    }

    @Override
    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<AdminChatResponse.MessagePage>> messages(
            @PathVariable Long operatorId,
            @PathVariable Long channelId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        ChatResult.MessagePage result = chatService.messages(operatorId, channelId, cursor, size);
        return ApiResponses.ok(AdminChatWebMapper.toMessagePage(result));
    }

    @Override
    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<AdminChatResponse.Message>> sendMessage(
            @PathVariable Long operatorId,
            @PathVariable Long channelId,
            @Valid @RequestBody AdminChatRequest.SendMessage request
    ) {
        ChatResult.Message result = chatService.sendMessage(
                operatorId,
                channelId,
                AdminChatWebMapper.toSendMessageCommand(request)
        );

        return ApiResponses.ok(AdminChatWebMapper.toMessage(result));
    }
}
