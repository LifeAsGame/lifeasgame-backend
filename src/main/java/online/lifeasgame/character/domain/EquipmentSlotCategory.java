package online.lifeasgame.character.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import online.lifeasgame.character.domain.error.EquipmentSlotError;
import online.lifeasgame.core.error.DomainException;

public enum EquipmentSlotCategory {
    WEAPON,        // WEAPON_MAIN / OFFHAND 묶음 정책 (양손/보조무기 규칙 등)
    HEAD, CHEST, LEGS, HANDS, FEET,
    NECK, RING, TRINKET
    ;

    public static EquipmentSlotCategory parse(String raw) {
        if (raw == null) {
            throw new DomainException(EquipmentSlotError.INVALID_EQUIPMENT_SLOT_CATEGORY, "EquipmentSlot category is null");
        }

        String norm = normalize(raw);

        if (norm.isEmpty()) {
            throw new DomainException(EquipmentSlotError.INVALID_EQUIPMENT_SLOT_CATEGORY, "EquipmentSlot category is blank");
        }

        try {
            return EquipmentSlotCategory.valueOf(norm);
        } catch (IllegalArgumentException e) {
            throw new DomainException(
                    EquipmentSlotError.INVALID_EQUIPMENT_SLOT_CATEGORY,
                    "Invalid EquipmentSlot category: " + raw + " (allowed: " + allowedList() + ")"
            );
        }
    }

    public static List<EquipmentSlotCategory> parse(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<String> invalid = new ArrayList<>();
        LinkedHashSet<EquipmentSlotCategory> parsed = new LinkedHashSet<>();

        for (String s : raw) {
            if (s == null) {
                continue;
            }

            String norm = normalize(s);

            if (norm.isEmpty()) {
                continue;
            }

            try {
                parsed.add(EquipmentSlotCategory.valueOf(norm));
            } catch (IllegalArgumentException e) {
                invalid.add(s);
            }
        }

        if (!invalid.isEmpty()) {
            throw new DomainException(
                    EquipmentSlotError.INVALID_EQUIPMENT_SLOT_CATEGORY,
                    "Invalid EquipmentSlot categories: " + invalid + " (allowed: " + allowedList() + ")"
            );
        }

        return List.copyOf(parsed);
    }

    private static String normalize(String s) {
        return s.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static String allowedList() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
