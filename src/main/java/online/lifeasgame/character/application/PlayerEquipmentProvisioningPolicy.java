package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotLifecycleStatus;
import online.lifeasgame.character.domain.error.EquipmentSlotError;
import online.lifeasgame.core.error.DomainException;

import java.util.List;

final class PlayerEquipmentProvisioningPolicy {

    static final String DEFINITION_VERSION = "1.0.0";
    static final List<String> REQUIRED_CODES = List.of(
            "HEAD",
            "NECK",
            "BODY",
            "WRIST",
            "RING_LEFT",
            "FEET",
            "AURA",
            "PROFILE_FRAME",
            "BADGE"
    );

    private PlayerEquipmentProvisioningPolicy() {
    }

    static List<EquipmentSlot> resolveRequiredDefinitions(
            List<EquipmentSlot> catalog
    ) {
        return REQUIRED_CODES.stream()
                .map(code -> resolveRequiredDefinition(catalog, code))
                .toList();
    }

    private static EquipmentSlot resolveRequiredDefinition(
            List<EquipmentSlot> catalog,
            String code
    ) {
        List<EquipmentSlot> matches = catalog.stream()
                .filter(slot -> code.equals(slot.getCode()))
                .filter(slot -> DEFINITION_VERSION.equals(
                        slot.getDefinitionVersion()
                ))
                .toList();
        if (matches.size() != 1) {
            throw authorityConflict();
        }

        EquipmentSlot slot = matches.getFirst();
        if (!slot.isEnabled()
                || slot.getLifecycleStatus()
                != EquipmentSlotLifecycleStatus.ACTIVE
                || !slot.isEagerOnLinkStart()) {
            throw authorityConflict();
        }
        return slot;
    }

    private static DomainException authorityConflict() {
        return new DomainException(
                EquipmentSlotError.EQUIPMENT_SLOT_AUTHORITY_CONFLICT
        );
    }
}
