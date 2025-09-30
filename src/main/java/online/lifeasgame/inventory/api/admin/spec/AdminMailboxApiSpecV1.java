package online.lifeasgame.inventory.api.admin.spec;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.admin.request.AdminMailboxRequest;
import online.lifeasgame.inventory.api.admin.reseponse.AdminMailboxResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Mailbox API V1 (Admin)")
public interface AdminMailboxApiSpecV1 {

    @Operation(summary = "우편 지급(관리자)", description = "특정 플레이어 우편함에 아이템을 지급합니다.")
    ResponseEntity<ApiResponse<AdminMailboxResponse.Slot>> deliverToMailbox(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminMailboxRequest.Deliver request
    );
}
