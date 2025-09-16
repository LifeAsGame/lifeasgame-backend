package online.lifeasgame.character.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.presentation.request.PlayerEquipmentRequest;
import online.lifeasgame.character.presentation.response.PlayerEquipmentResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PlayerEquipmentApiSpecV1 {

    @Operation(summary = "Player 장비 장착", description = "장비를 장착합니다.")
    ResponseEntity<ApiResponse<PlayerEquipmentResponse.EquippedEquipment>> equip(
            @PathVariable Long slotId,
            @RequestBody PlayerEquipmentRequest.EquipEquipment request
    );

    @Operation(summary = "Player 장비 해제", description = "장비를 해제합니다")
    ResponseEntity<ApiResponse<Long>> unEquip(
            @PathVariable Long slotId
    );
}
