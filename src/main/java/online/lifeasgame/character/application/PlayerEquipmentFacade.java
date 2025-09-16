package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerEquipmentFacade {

    public final CurrentPlayerAccessor currentPlayerAccessor;
    private final PlayerEquipmentService playerEquipmentService;

    public PlayerEquipmentResult.EquippedEquipment equip(PlayerEquipmentCommand.EquipEquipment command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return playerEquipmentService.equip(playerId, command);
    }

    public void unEquip(Long slotId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        playerEquipmentService.unEquip(playerId, slotId);
    }

    public List<PlayerEquipmentResult.PlayerEquipmentInfo> getPlayerEquipmentInfos() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return playerEquipmentService.getPlayerEquipmentInfos(playerId);
    }
}
