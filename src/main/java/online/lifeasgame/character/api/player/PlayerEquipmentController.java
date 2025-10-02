package online.lifeasgame.character.api.player;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerEquipmentFacade;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.application.result.PlayerEquipmentResult.PlayerEquipmentInfo;
import online.lifeasgame.character.api.player.mapper.PlayerEquipmentWebMapper;
import online.lifeasgame.character.api.player.request.PlayerEquipmentRequest;
import online.lifeasgame.character.api.player.response.PlayerEquipmentResponse;
import online.lifeasgame.character.api.player.spec.PlayerEquipmentApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerEquipmentController implements PlayerEquipmentApiSpecV1 {

    private final PlayerEquipmentFacade playerEquipmentFacade;

    @Override
    @PutMapping("/equipment/{slotId}")
    public ResponseEntity<ApiResponse<PlayerEquipmentResponse.EquippedEquipment>> equip(
            @PathVariable Long slotId,
            @RequestBody PlayerEquipmentRequest.EquipEquipment request
    ) {
        PlayerEquipmentResult.EquippedEquipment equippedEquipment = playerEquipmentFacade.equip(
                PlayerEquipmentWebMapper.toCommand(slotId, request)
        );

        return ApiResponses.ok(
                PlayerEquipmentWebMapper.toEquippedEquipment(equippedEquipment)
        );
    }

    @Override
    @GetMapping("/equipment")
    public ResponseEntity<ApiResponse<PlayerEquipmentResponse.PlayerEquipmentInfos>> playerEquipmentInfos() {
        List<PlayerEquipmentInfo> playerEquipmentInfos = playerEquipmentFacade.getPlayerEquipmentInfos();
        return ApiResponses.ok(
                PlayerEquipmentWebMapper.toPlayerEquipmentInfos(playerEquipmentInfos)
        );
    }

    @Override
    @DeleteMapping("/equipment/{slotId}")
    public ResponseEntity<ApiResponse<Long>> unEquip(
            @PathVariable Long slotId
    ) {
        playerEquipmentFacade.unEquip(slotId);
        return ApiResponses.deleted(slotId);
    }
}
