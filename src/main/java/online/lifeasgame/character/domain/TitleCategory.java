package online.lifeasgame.character.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import online.lifeasgame.character.domain.error.TitleError;
import online.lifeasgame.core.error.DomainException;

public enum TitleCategory {
    ACHIEVEMENT, EVENT, QUEST, RANKED, SPECIAL, OTHER;

    public static List<TitleCategory> parse(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<String> invalid = new ArrayList<>();
        List<TitleCategory> parsed = new ArrayList<>();

        for (String s : raw) {
            if (s == null) {
                continue;
            }

            String norm = normalize(s);

            if (norm.isEmpty()) {
                continue;
            }

            try {
                parsed.add(TitleCategory.valueOf(norm));
            } catch (IllegalArgumentException e) {
                invalid.add(s);
            }
        }

        if (!invalid.isEmpty()) {
            String allowed = Arrays.stream(values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

            throw new DomainException(
                    TitleError.INVALID_TITLE_CATEGORY,
                    "Invalid title categories: " + invalid + " (allowed: " + allowed + ")"
            );
        }

        return parsed.stream().distinct().toList();
    }

    private static String normalize(String s) {
        return s.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
