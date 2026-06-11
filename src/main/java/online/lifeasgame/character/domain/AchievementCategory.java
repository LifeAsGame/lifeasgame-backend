package online.lifeasgame.character.domain;

import online.lifeasgame.character.domain.error.AchievementError;
import online.lifeasgame.core.lang.EnumParsers;

import java.util.List;

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
        return EnumParsers.parseStrict(
                AchievementCategory.class,
                raw,
                AchievementError.INVALID_ACHIEVEMENT_CATEGORY,
                "Achievement category"
        );
    }

    public static List<AchievementCategory> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                AchievementCategory.class,
                raw,
                AchievementError.INVALID_ACHIEVEMENT_CATEGORY,
                "Achievement categories"
        );
    }
}
