package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand.Equip;
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
    public PlayerEquipmentResult.Equipped equip(Long playerId, Equip command) {
        if (playerEquipmentReader.existsItemInstance(command.itemInstanceId())) {
            throw new DomainException(PlayerEquipmentError.ALREADY_EQUIPPED_ITEM);
        }

        return PlayerEquipmentResult.Equipped.of(
                playerEquipmentWriter.equip(playerId, command.slotId(), command.itemInstanceId())
        );
    }

    @Transactional
    public void unEquip(Long playerId, Long slotId) {
        playerEquipmentWriter.unEquip(playerId, slotId);
    }

    public List<PlayerEquipmentResult.Info> getPlayerEquipmentInfos(Long playerId) {
        List<PlayerEquipment> playerEquipmentInfos = playerEquipmentReader.getPlayerEquipmentInfos(playerId);
        return playerEquipmentInfos.stream()
                .map(PlayerEquipmentResult.Info::from)
                .toList();
    }
}
