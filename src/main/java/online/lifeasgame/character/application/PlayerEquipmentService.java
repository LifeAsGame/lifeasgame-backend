package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand.Equip;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentAvailabilityApi;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentReadApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerEquipmentService {

    private final PlayerEquipmentWriter playerEquipmentWriter;
    private final PlayerEquipmentReader playerEquipmentReader;
    private final EquipmentSlotReader equipmentSlotReader;
    private final InventoryEquipmentReadApi inventoryEquipmentReadApi;
    private final InventoryEquipmentAvailabilityApi inventoryEquipmentAvailabilityApi;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public PlayerEquipmentResult.Equipped equip(Equip command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        EquipmentSlot slot = equipmentSlotReader.getByIdOrThrow(
                command.slotId()
        );
        InventoryEquipmentReadApi.OwnedEquipmentItem item =
                inventoryEquipmentReadApi.getOwnedItem(
                        playerId,
                        command.itemInstanceId()
                );
        EquipmentCompatibilityPolicy.validate(
                slot.getCategory(),
                item.category(),
                item.type(),
                item.equipmentCompatibilityKind()
        );
        playerEquipmentReader.assertNotEquipped(
                playerId,
                command.itemInstanceId()
        );

        PlayerEquipmentWriter.EquipmentReplacement replacement =
                playerEquipmentWriter.equip(
                        playerId,
                        command.slotId(),
                        command.itemInstanceId()
                );
        inventoryEquipmentAvailabilityApi.replaceEquippedItem(
                playerId,
                replacement.previousItemInstanceId(),
                command.itemInstanceId()
        );

        return PlayerEquipmentResult.Equipped.from(replacement.equipment());
    }

    @Transactional
    public void unEquip(Long slotId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Long itemInstanceId = playerEquipmentWriter.unEquip(playerId, slotId);
        if (itemInstanceId != null) {
            inventoryEquipmentAvailabilityApi.releaseEquippedItem(
                    playerId,
                    itemInstanceId
            );
        }
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
