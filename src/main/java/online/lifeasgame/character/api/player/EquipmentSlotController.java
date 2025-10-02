package online.lifeasgame.character.api.player;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.EquipmentSlotService;
import online.lifeasgame.character.application.result.EquipmentSlotResult;
import online.lifeasgame.character.api.player.mapper.EquipmentSlotWebMapper;
import online.lifeasgame.character.api.player.response.EquipmentSlotResponse;
import online.lifeasgame.character.api.player.spec.EquipmentSlotApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        List<EquipmentSlotResult.Info> infos = equipmentSlotService.getEquipmentSlots(categories, roles);
        return ApiResponses.ok(
                EquipmentSlotWebMapper.toEquipmentSlotInfos(infos)
        );
    }
}
