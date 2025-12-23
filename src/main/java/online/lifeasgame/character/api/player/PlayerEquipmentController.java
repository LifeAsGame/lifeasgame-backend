package online.lifeasgame.character.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.PlayerEquipmentWebMapper;
import online.lifeasgame.character.api.player.request.PlayerEquipmentRequest;
import online.lifeasgame.character.api.player.response.PlayerEquipmentResponse;
import online.lifeasgame.character.api.player.spec.PlayerEquipmentApiSpecV1;
import online.lifeasgame.character.application.PlayerEquipmentFacade;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerEquipmentController implements PlayerEquipmentApiSpecV1 {

    private final PlayerEquipmentFacade playerEquipmentFacade;

    @Override
    @GetMapping("/equipment")
    public ResponseEntity<ApiResponse<PlayerEquipmentResponse.Infos>> playerEquipmentInfos() {
        List<PlayerEquipmentResult.Info> results = playerEquipmentFacade.getPlayerEquipmentInfos();
        return ApiResponses.ok(PlayerEquipmentWebMapper.toInfos(results));
    }

    @Override
    @PutMapping("/equipment/{slotId}")
    public ResponseEntity<ApiResponse<PlayerEquipmentResponse.Equipped>> equip(
            @PathVariable Long slotId,
            @RequestBody PlayerEquipmentRequest.Equip request
    ) {
        PlayerEquipmentResult.Equipped result = playerEquipmentFacade.equip(
                PlayerEquipmentWebMapper.toEquipCommand(slotId, request)
        );

        return ApiResponses.ok(PlayerEquipmentWebMapper.toEquipped(result));
    }

    @Override
    @DeleteMapping("/equipment/{slotId}")
    public ResponseEntity<ApiResponse<Long>> unEquip(@PathVariable Long slotId) {
        playerEquipmentFacade.unEquip(slotId);
        return ApiResponses.deleted(slotId);
    }
}
