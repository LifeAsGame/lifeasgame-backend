package online.lifeasgame.quest.domain;

import java.time.Instant;

public record QuestBlueprint(
        QuestCode code,
        QuestCategory category,
        QuestTitle title,
        String descriptionMd,
        QuestTarget target,
        QuestReward reward,
        QuestRepeatRule repeatRule,
        Instant dueAt
) {
    public Quest instantiate() {
        return Quest.create(
                code.value(),
                category,
                title,
                descriptionMd,
                target,
                reward,
                repeatRule,
                dueAt
        );
    }
}
