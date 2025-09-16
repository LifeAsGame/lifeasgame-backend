package online.lifeasgame.character.presentation;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerEquipmentFacade;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.presentation.mapper.PlayerEquipmentWebMapper;
import online.lifeasgame.character.presentation.request.PlayerEquipmentRequest;
import online.lifeasgame.character.presentation.response.PlayerEquipmentResponse;
import online.lifeasgame.character.presentation.spec.PlayerEquipmentApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    @DeleteMapping("/equipment/{slotId}")
    public ResponseEntity<ApiResponse<Long>> unEquip(
            @PathVariable Long slotId
    ) {
        playerEquipmentFacade.unEquip(slotId);
        return ApiResponses.deleted(slotId);
    }
}
