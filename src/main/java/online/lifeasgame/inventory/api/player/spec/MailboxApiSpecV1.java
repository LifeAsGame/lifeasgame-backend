package online.lifeasgame.inventory.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.player.request.MailboxRequest;
import online.lifeasgame.inventory.api.player.response.MailboxResponse.Mails;
import online.lifeasgame.inventory.api.player.response.MailboxResponse.Slot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Mailbox API V1")
public interface MailboxApiSpecV1 {

    @Operation(summary = "우편 목록 조회", description = "플레이어 우편함 목록을 조회합니다.")
    ResponseEntity<ApiResponse<Mails>> list();

    @Operation(summary = "우편 지급", description = "아이템을 플레이어의 우편함에 지급합니다.")
    ResponseEntity<ApiResponse<Slot>> deliver(@Valid @RequestBody MailboxRequest.Deliver request);

    @Operation(summary = "우편 수령", description = "우편 슬롯에서 인벤토리로 수령합니다.")
    ResponseEntity<ApiResponse<Void>> claim(@Valid @RequestBody MailboxRequest.Claim request);
}
