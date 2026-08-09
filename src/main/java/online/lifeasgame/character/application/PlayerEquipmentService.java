package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand.Equip;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerEquipmentService {

    private final PlayerEquipmentWriter playerEquipmentWriter;
    private final PlayerEquipmentReader playerEquipmentReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public PlayerEquipmentResult.Equipped equip(Equip command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        playerEquipmentReader.assertNotEquipped(playerId, command.slotId(), command.itemInstanceId());

        PlayerEquipment playerEquipment = playerEquipmentWriter.equip(
                playerId,
                command.slotId(),
                command.itemInstanceId()
        );

        return PlayerEquipmentResult.Equipped.from(playerEquipment);
    }

    @Transactional
    public void unEquip(Long slotId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        playerEquipmentWriter.unEquip(playerId, slotId);
    }

    @Transactional(readOnly = true)
    public List<PlayerEquipmentResult.Info> getPlayerEquipmentInfos() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<PlayerEquipment> playerEquipmentInfos = playerEquipmentReader.getByPlayerId(playerId);
        return playerEquipmentInfos.stream()
                .map(PlayerEquipmentResult.Info::from)
                .toList();
    }

    @Transactional
    public void init(Long playerId) {
        for (int i = 1; i < 12; i++) {
            playerEquipmentWriter.create(playerId, (long) i);
        }
    }
}
