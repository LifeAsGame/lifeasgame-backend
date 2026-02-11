package online.lifeasgame.social.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.request.PlayerChatRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PlayerChatApiDraftSpecV1 {

    @Operation(summary = "읽음 처리(텍스트 UI에서 '새 메시지' 제거용)")
    ResponseEntity<ApiResponse<Void>> read(
            @PathVariable Long channelId,
            @Valid @RequestBody PlayerChatRequest.Read request
    );
}
