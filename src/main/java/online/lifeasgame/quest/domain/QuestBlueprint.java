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
        RewardProfileRef rewardProfileRef,
        QuestSemanticCategory semanticCategory,
        QuestProgressSource progressSource,
        QuestRepeatRule repeatPolicy,
        QuestRoleTemplateRef roleTemplateRef
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
        boolean finalContractRequested = semanticCategory != null
                || progressSource != null
                || repeatPolicy != null
                || roleTemplateRef != null;
        if (finalContractRequested) {
            validateFinalContract(
                    semanticCategory,
                    progressSource,
                    repeatPolicy
            );
            if (rewardProfileRef == null) {
                throw new DomainException(
                        QuestError.QUEST_REWARD_PROFILE_CODE_REQUIRED
                );
            }
            if (repeatRule != repeatPolicy) {
                throw new DomainException(
                        QuestError.QUEST_REPEAT_CONTRACT_CONFLICT
                );
            }
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
                null,
                null,
                null,
                null,
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
                null,
                null,
                null,
                null,
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
                rewardProfileRef,
                null,
                null,
                null,
                null
        );
    }

    public static QuestBlueprint finalContract(
            QuestCode code,
            int definitionVersion,
            QuestCategory category,
            QuestSemanticCategory semanticCategory,
            QuestTitle title,
            String descriptionMd,
            QuestTarget target,
            QuestProgressSource progressSource,
            RewardProfileRef rewardProfileRef,
            QuestRepeatRule repeatPolicy,
            QuestRoleTemplateRef roleTemplateRef,
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
                repeatPolicy,
                dueAt,
                completionPolicy,
                definitionVersion,
                rewardProfileRef,
                semanticCategory,
                progressSource,
                repeatPolicy,
                roleTemplateRef
        );
    }

    public Quest instantiate() {
        if (isFinalContract()) {
            return Quest.createDefinition(
                    code.value(),
                    definitionVersion,
                    category,
                    semanticCategory,
                    title,
                    descriptionMd,
                    target,
                    progressSource,
                    rewardProfileRef,
                    repeatPolicy,
                    roleTemplateRef,
                    completionPolicy,
                    dueAt
            );
        }
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

    public boolean isFinalContract() {
        return semanticCategory != null
                && progressSource != null
                && repeatPolicy != null;
    }

    public String roleTemplateCodeOrNull() {
        return roleTemplateRef == null ? null : roleTemplateRef.code();
    }

    private static void validateFinalContract(
            QuestSemanticCategory semanticCategory,
            QuestProgressSource progressSource,
            QuestRepeatRule repeatPolicy
    ) {
        if (semanticCategory == null) {
            throw new DomainException(
                    QuestError.QUEST_SEMANTIC_CATEGORY_REQUIRED
            );
        }
        if (progressSource == null) {
            throw new DomainException(
                    QuestError.QUEST_PROGRESS_SOURCE_REQUIRED
            );
        }
        if (repeatPolicy == null) {
            throw new DomainException(QuestError.QUEST_REPEAT_POLICY_REQUIRED);
        }
        if (!repeatPolicy.isFinalPolicy()) {
            throw new DomainException(QuestError.INVALID_QUEST_REPEAT_POLICY);
        }
    }
}
