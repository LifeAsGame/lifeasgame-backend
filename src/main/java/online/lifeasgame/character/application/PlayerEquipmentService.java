package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand.EquipEquipment;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerEquipmentService {

    private final PlayerEquipmentWriter playerEquipmentWriter;
    private final PlayerEquipmentReader playerEquipmentReader;

    @Transactional
    public PlayerEquipmentResult.EquippedEquipment equip(Long playerId, EquipEquipment command) {
        if (playerEquipmentReader.existsItemInstance(command.itemInstanceId())) {
            throw new DomainException(PlayerEquipmentError.ALREADY_EQUIPPED_ITEM);
        }

        return PlayerEquipmentResult.EquippedEquipment.of(
                playerEquipmentWriter.equip(playerId, command.slotId(), command.itemInstanceId())
        );
    }

    @Transactional
    public void unEquip(Long playerId, Long slotId) {
        playerEquipmentWriter.unEquip(playerId, slotId);
    }

    public List<PlayerEquipmentResult.PlayerEquipmentInfo> getPlayerEquipmentInfos(Long playerId) {
        List<PlayerEquipment> playerEquipmentInfos = playerEquipmentReader.getPlayerEquipmentInfos(playerId);
        return playerEquipmentInfos.stream()
                .map(PlayerEquipmentResult.PlayerEquipmentInfo::from)
                .toList();
    }
}
