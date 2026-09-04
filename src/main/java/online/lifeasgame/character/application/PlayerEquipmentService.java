package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand.Equip;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentAvailabilityApi;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentReadApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        if (!slot.supportsEquipmentCommand()) {
            throw new DomainException(
                    PlayerEquipmentError.UNSUPPORTED_EQUIPMENT_SLOT
            );
        }
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
        Map<Long, EquipmentSlot> slotsById = equipmentSlotReader.getAll()
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        EquipmentSlot::getId,
                        Function.identity()
                ));
        List<PlayerEquipment> playerEquipmentInfos =
                playerEquipmentReader.getByPlayerId(playerId);
        return playerEquipmentInfos.stream()
                .filter(equipment -> slotsById.containsKey(
                        equipment.getSlotId()
                ))
                .filter(equipment -> slotsById.get(equipment.getSlotId())
                        .isVisiblePlayerEquipmentSlot())
                .sorted((left, right) -> Integer.compare(
                        slotsById.get(left.getSlotId()).getSortOrder(),
                        slotsById.get(right.getSlotId()).getSortOrder()
                ))
                .map(equipment -> PlayerEquipmentResult.Info.from(
                        equipment,
                        slotsById.get(equipment.getSlotId())
                ))
                .toList();
    }

}
