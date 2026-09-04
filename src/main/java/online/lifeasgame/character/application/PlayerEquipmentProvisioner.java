package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerEquipmentProvisioner {

    private final EquipmentSlotReader slotReader;
    private final PlayerEquipmentReader equipmentReader;
    private final PlayerEquipmentWriter equipmentWriter;

    public void provisionAndVerify(Long playerId) {
        List<EquipmentSlot> catalog = slotReader.getAll();
        List<EquipmentSlot> required =
                PlayerEquipmentProvisioningPolicy
                        .resolveRequiredDefinitions(catalog);
        Map<Long, EquipmentSlot> slotsById = catalog.stream()
                .collect(Collectors.toUnmodifiableMap(
                        EquipmentSlot::getId,
                        Function.identity()
                ));

        List<EquipmentSlot> missing = missingRequiredSlots(
                required,
                equipmentReader.getByPlayerIdForUpdate(playerId),
                slotsById
        );
        if (!missing.isEmpty()) {
            equipmentWriter.createEmpty(
                    playerId,
                    missing.stream().map(EquipmentSlot::getId).toList()
            );
        }

        if (!missingRequiredSlots(
                required,
                equipmentReader.getByPlayerIdForUpdate(playerId),
                slotsById
        ).isEmpty()) {
            throw onboardingConflict();
        }
    }

    private List<EquipmentSlot> missingRequiredSlots(
            List<EquipmentSlot> required,
            List<PlayerEquipment> equipment,
            Map<Long, EquipmentSlot> slotsById
    ) {
        Map<String, List<PlayerEquipment>> rowsByRequiredCode = equipment
                .stream()
                .filter(row -> slotsById.containsKey(row.getSlotId()))
                .filter(row -> PlayerEquipmentProvisioningPolicy
                        .REQUIRED_CODES.contains(
                                slotsById.get(row.getSlotId()).getCode()
                        ))
                .collect(Collectors.groupingBy(row ->
                        slotsById.get(row.getSlotId()).getCode()
                ));

        List<EquipmentSlot> missing = new ArrayList<>();
        for (EquipmentSlot requiredSlot : required) {
            List<PlayerEquipment> rows = rowsByRequiredCode.getOrDefault(
                    requiredSlot.getCode(),
                    List.of()
            );
            if (rows.isEmpty()) {
                missing.add(requiredSlot);
                continue;
            }
            if (rows.size() != 1
                    || !rows.getFirst().getSlotId()
                    .equals(requiredSlot.getId())) {
                throw onboardingConflict();
            }
        }
        return missing;
    }

    private DomainException onboardingConflict() {
        return new DomainException(
                PlayerEquipmentError.PLAYER_EQUIPMENT_ONBOARDING_CONFLICT
        );
    }
}
