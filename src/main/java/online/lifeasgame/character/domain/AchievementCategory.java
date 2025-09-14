package online.lifeasgame.character.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import online.lifeasgame.character.domain.error.AchievementError;
import online.lifeasgame.core.error.DomainException;

public enum AchievementCategory{
    STORY,        // 스토리/챕터 클리어
    COMBAT,       // 전투/처치/보스
    EXPLORATION,  // 지역/지도/발견
    COLLECTION,   // 수집/아이템/도감
    SOCIAL,       // 친구/파티/길드
    ECONOMY,      // 골드/거래/상점
    SKILL,        // 스킬/레벨/특성
    DAILY         // 일일 도전/출석
    ;

    public static AchievementCategory parse(String raw) {
        if (raw == null) {
            throw new DomainException(AchievementError.INVALID_ACHIEVEMENT_CATEGORY, "Achievement category is null");
        }

        String norm = normalize(raw);

        if (norm.isEmpty()) {
            throw new DomainException(AchievementError.INVALID_ACHIEVEMENT_CATEGORY, "Achievement category is blank");
        }

        try {
            return AchievementCategory.valueOf(norm);
        } catch (IllegalArgumentException e) {
            throw new DomainException(
                    AchievementError.INVALID_ACHIEVEMENT_CATEGORY,
                    "Invalid Achievement category: " + raw + " (allowed: " + allowedList() + ")"
            );
        }
    }

    public static List<AchievementCategory> parse(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<String> invalid = new ArrayList<>();
        LinkedHashSet<AchievementCategory> parsed = new LinkedHashSet<>();

        for (String s : raw) {
            if (s == null) {
                continue;
            }

            String norm = normalize(s);

            if (norm.isEmpty()) {
                continue;
            }

            try {
                parsed.add(AchievementCategory.valueOf(norm));
            } catch (IllegalArgumentException e) {
                invalid.add(s);
            }
        }

        if (!invalid.isEmpty()) {
            throw new DomainException(
                    AchievementError.INVALID_ACHIEVEMENT_CATEGORY,
                    "Invalid Achievement categories: " + invalid + " (allowed: " + allowedList() + ")"
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
