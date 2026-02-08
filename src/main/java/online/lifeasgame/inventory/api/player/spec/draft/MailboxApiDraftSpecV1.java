package online.lifeasgame.inventory.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.player.response.MailboxResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface MailboxApiDraftSpecV1 {

    @Operation(summary = "우편함 화면 조회", description = "capacity/used/free 메타 + 메일 엔트리(옵션: 아이템/내구도/속성 포함)를 조회합니다.")
    ResponseEntity<ApiResponse<MailboxResponse.View>> view(
            @RequestParam(defaultValue = "true") boolean includeItem,
            @RequestParam(defaultValue = "true") boolean includeInstanceAttrs
    );
}
