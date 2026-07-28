package online.lifeasgame.quest.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.quest.domain.error.QuestError;

public enum QuestSemanticCategory {
    GROWTH,
    RECORD,
    RECOVERY,
    RELATION,
    ROLE;

    public static QuestSemanticCategory parse(String raw) {
        return EnumParsers.parseStrict(
                QuestSemanticCategory.class,
                raw,
                QuestError.INVALID_QUEST_SEMANTIC_CATEGORY,
                "Quest semantic category"
        );
    }

    public static QuestSemanticCategory parseNullable(String raw) {
        return raw == null ? null : parse(raw);
    }
}
