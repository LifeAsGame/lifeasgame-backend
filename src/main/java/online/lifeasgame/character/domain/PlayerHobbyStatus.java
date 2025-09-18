package online.lifeasgame.character.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import online.lifeasgame.character.domain.error.PlayerHobbyError;
import online.lifeasgame.core.error.DomainException;

public enum PlayerHobbyStatus {
    ACTIVE, PAUSED, DROPPED
    ;

    public static PlayerHobbyStatus parse(String raw) {
        if (raw == null) {
            throw new DomainException(PlayerHobbyError.INVALID_PLAYER_HOBBY_STATUS, "PlayerHobby status is null");
        }

        String norm = normalize(raw);

        if (norm.isEmpty()) {
            throw new DomainException(PlayerHobbyError.INVALID_PLAYER_HOBBY_STATUS, "PlayerHobby status is blank");
        }

        try {
            return PlayerHobbyStatus.valueOf(norm);
        } catch (IllegalArgumentException e) {
            throw new DomainException(
                    PlayerHobbyError.INVALID_PLAYER_HOBBY_STATUS,
                    "Invalid PlayerHobby Status: " + raw + " (allowed: " + allowedList() + ")"
            );
        }
    }

    public static List<PlayerHobbyStatus> parse(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<String> invalid = new ArrayList<>();
        LinkedHashSet<PlayerHobbyStatus> parsed = new LinkedHashSet<>();

        for (String s : raw) {
            if (s == null) {
                continue;
            }

            String norm = normalize(s);

            if (norm.isEmpty()) {
                continue;
            }

            try {
                parsed.add(PlayerHobbyStatus.valueOf(norm));
            } catch (IllegalArgumentException e) {
                invalid.add(s);
            }
        }

        if (!invalid.isEmpty()) {
            throw new DomainException(
                    PlayerHobbyError.INVALID_PLAYER_HOBBY_STATUS,
                    "Invalid PlayerHobby categories: " + invalid + " (allowed: " + allowedList() + ")"
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
