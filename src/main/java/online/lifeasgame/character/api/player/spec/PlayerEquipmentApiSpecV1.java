package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.player.request.PlayerEquipmentRequest;
import online.lifeasgame.character.api.player.response.PlayerEquipmentResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PlayerEquipmentApiSpecV1 {

    @Operation(summary = "Player 장착 현황 조회", description = "모든 슬롯 + 장착 itemInstanceId(없으면 null)를 내려줍니다.")
    ResponseEntity<ApiResponse<PlayerEquipmentResponse.Infos>> playerEquipmentInfos();

    @Operation(summary = "장착", description = "slotId에 itemInstanceId를 장착합니다.")
    ResponseEntity<ApiResponse<PlayerEquipmentResponse.Equipped>> equip(
            @PathVariable Long slotId,
            @Valid @RequestBody PlayerEquipmentRequest.Equip request
    );

    @Operation(summary = "해제", description = "slotId의 장착을 해제합니다.")
    ResponseEntity<ApiResponse<PlayerEquipmentResponse.UnEquipped>> unEquip(
            @PathVariable Long slotId
    );
}
