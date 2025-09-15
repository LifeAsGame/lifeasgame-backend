package online.lifeasgame.character.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import online.lifeasgame.character.domain.error.EquipmentSlotError;
import online.lifeasgame.core.error.DomainException;

public enum EquipmentSlotRole {
    SINGLE,   // 대부분의 단일 슬롯
    MAIN,     // 주무기
    OFFHAND,  // 보조무기/방패
    LEFT,     // 반지(좌)
    RIGHT     // 반지(우)
    ;

    public static EquipmentSlotRole parse(String raw) {
        if (raw == null) {
            throw new DomainException(EquipmentSlotError.INVALID_EQUIPMENT_SLOT_ROLE, "EquipmentSlot Role is null");
        }

        String norm = normalize(raw);

        if (norm.isEmpty()) {
            throw new DomainException(EquipmentSlotError.INVALID_EQUIPMENT_SLOT_ROLE, "EquipmentSlot Role is blank");
        }

        try {
            return EquipmentSlotRole.valueOf(norm);
        } catch (IllegalArgumentException e) {
            throw new DomainException(
                    EquipmentSlotError.INVALID_EQUIPMENT_SLOT_ROLE,
                    "Invalid EquipmentSlot Role: " + raw + " (allowed: " + allowedList() + ")"
            );
        }
    }

    public static List<EquipmentSlotRole> parse(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<String> invalid = new ArrayList<>();
        LinkedHashSet<EquipmentSlotRole> parsed = new LinkedHashSet<>();

        for (String s : raw) {
            if (s == null) {
                continue;
            }

            String norm = normalize(s);

            if (norm.isEmpty()) {
                continue;
            }

            try {
                parsed.add(EquipmentSlotRole.valueOf(norm));
            } catch (IllegalArgumentException e) {
                invalid.add(s);
            }
        }

        if (!invalid.isEmpty()) {
            throw new DomainException(
                    EquipmentSlotError.INVALID_EQUIPMENT_SLOT_ROLE,
                    "Invalid EquipmentSlot roles: " + invalid + " (allowed: " + allowedList() + ")"
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
