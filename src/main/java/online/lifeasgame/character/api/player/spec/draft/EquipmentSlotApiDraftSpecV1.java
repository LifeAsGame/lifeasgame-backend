package online.lifeasgame.character.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.EquipmentSlotResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface EquipmentSlotApiDraftSpecV1 {

    @Operation(summary = "Equipment Slot 단건 조회", description = "슬롯 상세(텍스트 UI 상세보기용)")
    ResponseEntity<ApiResponse<EquipmentSlotResponse.Info>> equipmentSlotInfo(
            @PathVariable Long slotId
    );
}
