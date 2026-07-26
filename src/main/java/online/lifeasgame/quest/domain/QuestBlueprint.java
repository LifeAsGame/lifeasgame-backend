package online.lifeasgame.quest.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;

import java.time.Instant;

public record QuestBlueprint(
        QuestCode code,
        QuestCategory category,
        QuestTitle title,
        String descriptionMd,
        QuestTarget target,
        QuestReward reward,
        QuestRepeatRule repeatRule,
        Instant dueAt,
        QuestCompletionPolicy completionPolicy,
        int definitionVersion,
        RewardProfileRef rewardProfileRef
) {
    public QuestBlueprint {
        completionPolicy = QuestCompletionPolicy.defaultIfNull(completionPolicy);
        if (definitionVersion < 1) {
            throw new DomainException(QuestError.QUEST_DEFINITION_VERSION_INVALID);
        }
        if (reward == null && rewardProfileRef == null) {
            throw new DomainException(QuestError.QUEST_REWARD_PROFILE_CODE_REQUIRED);
        }
        if (reward != null && rewardProfileRef != null) {
            throw new DomainException(QuestError.QUEST_REWARD_CONTRACT_CONFLICT);
        }
    }

    public QuestBlueprint(
            QuestCode code,
            QuestCategory category,
            QuestTitle title,
            String descriptionMd,
            QuestTarget target,
            QuestReward reward,
            QuestRepeatRule repeatRule,
            Instant dueAt,
            QuestCompletionPolicy completionPolicy
    ) {
        this(
                code,
                category,
                title,
                descriptionMd,
                target,
                reward,
                repeatRule,
                dueAt,
                completionPolicy,
                1,
                null
        );
    }

    public QuestBlueprint(
            QuestCode code,
            QuestCategory category,
            QuestTitle title,
            String descriptionMd,
            QuestTarget target,
            QuestReward reward,
            QuestRepeatRule repeatRule,
            Instant dueAt
    ) {
        this(
                code,
                category,
                title,
                descriptionMd,
                target,
                reward,
                repeatRule,
                dueAt,
                QuestCompletionPolicy.AUTO,
                1,
                null
        );
    }

    public static QuestBlueprint profileBased(
            QuestCode code,
            int definitionVersion,
            QuestCategory category,
            QuestTitle title,
            String descriptionMd,
            QuestTarget target,
            RewardProfileRef rewardProfileRef,
            QuestRepeatRule repeatRule,
            Instant dueAt,
            QuestCompletionPolicy completionPolicy
    ) {
        return new QuestBlueprint(
                code,
                category,
                title,
                descriptionMd,
                target,
                null,
                repeatRule,
                dueAt,
                completionPolicy,
                definitionVersion,
                rewardProfileRef
        );
    }

    public Quest instantiate() {
        if (rewardProfileRef != null) {
            return Quest.createDefinition(
                    code.value(),
                    definitionVersion,
                    category,
                    title,
                    descriptionMd,
                    target,
                    rewardProfileRef,
                    repeatRule,
                    completionPolicy,
                    dueAt
            );
        }
        return Quest.create(
                code.value(),
                category,
                title,
                descriptionMd,
                target,
                reward,
                repeatRule,
                completionPolicy,
                dueAt
        );
    }

    public boolean usesRewardProfile() {
        return rewardProfileRef != null;
    }

    public String rewardProfileCodeOrNull() {
        return rewardProfileRef == null ? null : rewardProfileRef.code();
    }
}
