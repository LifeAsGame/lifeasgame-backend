package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand.Equip;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.domain.PlayerEquipment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerEquipmentService {

    private final PlayerEquipmentWriter playerEquipmentWriter;
    private final PlayerEquipmentReader playerEquipmentReader;

    @Transactional
    public PlayerEquipmentResult.Equipped equip(Long playerId, Equip command) {
        playerEquipmentReader.assertNotExistsByItemInstanceId(command.itemInstanceId());

        PlayerEquipment playerEquipment = playerEquipmentWriter.equip(
                playerId,
                command.slotId(),
                command.itemInstanceId()
        );

        return PlayerEquipmentResult.Equipped.from(playerEquipment);
    }

    @Transactional
    public void unEquip(Long playerId, Long slotId) {
        playerEquipmentWriter.unEquip(playerId, slotId);
    }

    public List<PlayerEquipmentResult.Info> getPlayerEquipmentInfos(Long playerId) {
        List<PlayerEquipment> playerEquipmentInfos = playerEquipmentReader.getByPlayerId(playerId);
        return playerEquipmentInfos.stream()
                .map(PlayerEquipmentResult.Info::from)
                .toList();
    }
}
