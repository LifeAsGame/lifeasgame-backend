package online.lifeasgame.character.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import online.lifeasgame.character.api.response.EquipmentSlotResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface EquipmentSlotApiSpecV1 {

    @Operation(summary = "EquipmentSlot 목록 조회", description = "EquipmentSlot 목록 조회: category 설정 가능")
    ResponseEntity<ApiResponse<EquipmentSlotResponse.EquipmentSlotInfos>> EquipmentSlotInfos(
            @RequestParam(name = "category", required = false) List<String> categories,
            @RequestParam(name = "role", required = false) List<String> roles
    );
}
