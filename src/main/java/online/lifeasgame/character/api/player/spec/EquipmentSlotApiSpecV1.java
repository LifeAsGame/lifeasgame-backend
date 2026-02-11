package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.EquipmentSlotResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface EquipmentSlotApiSpecV1 {

    @Operation(summary = "Equipment Slot 목록 조회", description = "장착 슬롯 목록. role/category 필터 가능")
    ResponseEntity<ApiResponse<EquipmentSlotResponse.Infos>> EquipmentSlotInfos(
            @RequestParam(name = "category", required = false) List<String> categories,
            @RequestParam(name = "role", required = false) List<String> roles
    );

    @Operation(summary = "Equipment Slot 단건 조회", description = "슬롯 상세(텍스트 UI 상세보기용)")
    ResponseEntity<ApiResponse<EquipmentSlotResponse.Info>> equipmentSlotInfo(
            @PathVariable Long slotId
    );
}
