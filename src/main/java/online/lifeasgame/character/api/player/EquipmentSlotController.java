package online.lifeasgame.character.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.EquipmentSlotWebMapper;
import online.lifeasgame.character.api.player.response.EquipmentSlotResponse;
import online.lifeasgame.character.api.player.spec.EquipmentSlotApiSpecV1;
import online.lifeasgame.character.application.EquipmentSlotService;
import online.lifeasgame.character.application.result.EquipmentSlotResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/equip-slots")
public class EquipmentSlotController implements EquipmentSlotApiSpecV1 {

    private final EquipmentSlotService equipmentSlotService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<EquipmentSlotResponse.Infos>> EquipmentSlotInfos(
            @RequestParam(name = "category", required = false) List<String> categories,
            @RequestParam(name = "role", required = false) List<String> roles
    ) {
        List<EquipmentSlotResult.Info> results = equipmentSlotService.getEquipmentSlots(categories, roles);
        return ApiResponses.ok(EquipmentSlotWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{slotId}")
    public ResponseEntity<ApiResponse<EquipmentSlotResponse.Info>> equipmentSlotInfo(
            @PathVariable Long slotId
    ) {
        EquipmentSlotResult.Info result = equipmentSlotService.getEquipmentSlot(slotId);
        return ApiResponses.ok(EquipmentSlotWebMapper.toInfo(result));
    }
}
