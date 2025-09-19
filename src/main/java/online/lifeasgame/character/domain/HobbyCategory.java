package online.lifeasgame.character.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import online.lifeasgame.character.domain.error.HobbyError;
import online.lifeasgame.core.error.DomainException;

public enum HobbyCategory {
    FITNESS,        // 헬스/러닝/홈트/크로스핏 등
    SPORTS,         // 구기/라켓/격투/클라이밍 등
    OUTDOORS,       // 등산/캠핑/낚시/자전거/트레킹
    MUSIC,          // 악기/보컬/작곡/디제잉
    ARTS,           // 드로잉/페인팅/캘리/디자인
    CRAFTS,         // 목공/공예/프라모델/레고/DIY
    GAMING,         // PC/콘솔/모바일/아케이드
    BOARD_GAMES,    // 보드게임/TCG/마작 등
    TECH,           // 코딩/로봇/전자공작/3D프린팅
    COOKING,        // 요리/집밥/바베큐
    BAKING,         // 제과제빵/디저트/베이킹
    PHOTOGRAPHY,    // 사진/영상/드론/필름
    READING,        // 독서/북클럽/서평
    WRITING,        // 글쓰기/블로그/에세이
    LANGUAGE,       // 외국어 학습/회화/자격증
    TRAVEL,         // 여행/도시탐방/맛집투어
    WELLNESS,       // 명상/요가/마인드풀니스/호흡
    VOLUNTEERING    // 봉사/기부/커뮤니티 활동
    ;

    public static HobbyCategory parse(String raw) {
        if (raw == null) {
            throw new DomainException(HobbyError.INVALID_HOBBY_CATEGORY, "Hobby category is null");
        }

        String norm = normalize(raw);

        if (norm.isEmpty()) {
            throw new DomainException(HobbyError.INVALID_HOBBY_CATEGORY, "Hobby category is blank");
        }

        try {
            return HobbyCategory.valueOf(norm);
        } catch (IllegalArgumentException e) {
            throw new DomainException(
                    HobbyError.INVALID_HOBBY_CATEGORY,
                    "Invalid Hobby category: " + raw + " (allowed: " + allowedList() + ")"
            );
        }
    }

    public static List<HobbyCategory> parse(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<String> invalid = new ArrayList<>();
        LinkedHashSet<HobbyCategory> parsed = new LinkedHashSet<>();

        for (String s : raw) {
            if (s == null) {
                continue;
            }

            String norm = normalize(s);

            if (norm.isEmpty()) {
                continue;
            }

            try {
                parsed.add(HobbyCategory.valueOf(norm));
            } catch (IllegalArgumentException e) {
                invalid.add(s);
            }
        }

        if (!invalid.isEmpty()) {
            throw new DomainException(
                    HobbyError.INVALID_HOBBY_CATEGORY,
                    "Invalid Hobby categories: " + invalid + " (allowed: " + allowedList() + ")"
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
