package online.lifeasgame.quest.application.blueprint;

import online.lifeasgame.quest.domain.QuestBlueprint;
import online.lifeasgame.quest.domain.QuestCompletionPolicy;
import online.lifeasgame.quest.domain.QuestProgressSource;
import online.lifeasgame.quest.domain.QuestTarget;
import online.lifeasgame.quest.domain.QuestTargetType;
import online.lifeasgame.quest.domain.QuestTitle;
import online.lifeasgame.quest.domain.RewardProfileRef;
import online.lifeasgame.quest.domain.seed.QuestContentTargetUnit;
import online.lifeasgame.quest.domain.seed.QuestProgressSourceType;
import online.lifeasgame.quest.domain.seed.SeedLevel1QuestDefinition;

public final class SeedLevel1QuestBlueprintAdapter {

    private SeedLevel1QuestBlueprintAdapter() {
    }

    public static QuestBlueprint toBlueprint(
            SeedLevel1QuestDefinition definition
    ) {
        return QuestBlueprint.finalContract(
                definition.questCode(),
                definition.definitionVersion(),
                definition.semanticCategory(),
                QuestTitle.of(definition.displayNameKo()),
                definition.longDescriptionKo(),
                QuestTarget.of(
                        targetType(definition.targetUnit()),
                        definition.targetValue()
                ),
                progressSource(definition.progressSourceType()),
                RewardProfileRef.of(definition.rewardProfileCode()),
                definition.repeatPolicy(),
                null,
                null,
                completionPolicy(definition.autoComplete())
        );
    }

    private static QuestTargetType targetType(
            QuestContentTargetUnit targetUnit
    ) {
        return switch (targetUnit) {
            case DISTINCT_LIFELOG, WEEKLY_REFLECTION ->
                    QuestTargetType.COUNT;
            case MINUTE_INTENT -> QuestTargetType.MINUTES;
        };
    }

    private static QuestProgressSource progressSource(
            QuestProgressSourceType sourceType
    ) {
        return switch (sourceType) {
            case DURABLE_OUTBOX_FACT ->
                    QuestProgressSource.RECORD_CREATED;
            case USER_CONFIRMATION ->
                    QuestProgressSource.MANUAL_CHECK;
        };
    }

    private static QuestCompletionPolicy completionPolicy(
            boolean autoComplete
    ) {
        return autoComplete
                ? QuestCompletionPolicy.AUTO
                : QuestCompletionPolicy.USER_CONFIRM;
    }
}
