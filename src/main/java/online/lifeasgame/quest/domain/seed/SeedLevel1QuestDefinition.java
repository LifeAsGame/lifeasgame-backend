package online.lifeasgame.quest.domain.seed;

import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestRepeatRule;
import online.lifeasgame.quest.domain.QuestSemanticCategory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SeedLevel1QuestDefinition(
        QuestCode questCode,
        int seedLevel,
        QuestContentPriority priority,
        int definitionVersion,
        QuestDefinitionStatus status,
        String displayNameKo,
        String shortDescriptionKo,
        String longDescriptionKo,
        QuestSemanticCategory semanticCategory,
        QuestProgressMode progressMode,
        QuestProgressSourceType progressSourceType,
        String sourceEventType,
        String sourceEntityType,
        String sourceOwnerRule,
        int targetValue,
        QuestContentTargetUnit targetUnit,
        boolean autoComplete,
        QuestRepeatRule repeatPolicy,
        QuestContentPeriodBoundary periodBoundary,
        QuestContentTimezonePolicy timezonePolicy,
        String timezoneFallback,
        QuestRoleContextPolicy roleContextPolicy,
        Set<String> allowedRoleTypes,
        String rewardProfileCode,
        boolean manualCheckAllowed,
        boolean manualCheckRequiresMemo,
        String cancellationPolicy,
        String failurePressurePolicy,
        String recommendedNextAction,
        int sortOrder,
        String iconKey,
        String colorToken,
        String resultCopyKey,
        String emptyStateCopyKey,
        List<String> notes
) {

    public SeedLevel1QuestDefinition {
        Objects.requireNonNull(questCode, "questCode");
        if (seedLevel != 1) {
            throw new IllegalArgumentException("seedLevel must be 1");
        }
        if (priority != QuestContentPriority.P0) {
            throw new IllegalArgumentException("priority must be P0");
        }
        if (definitionVersion < 1) {
            throw new IllegalArgumentException(
                    "definitionVersion must be at least 1"
            );
        }
        if (status != QuestDefinitionStatus.ACTIVE) {
            throw new IllegalArgumentException("status must be ACTIVE");
        }
        requireText(displayNameKo, "displayNameKo");
        requireText(shortDescriptionKo, "shortDescriptionKo");
        requireText(longDescriptionKo, "longDescriptionKo");
        Objects.requireNonNull(semanticCategory, "semanticCategory");
        Objects.requireNonNull(progressMode, "progressMode");
        Objects.requireNonNull(progressSourceType, "progressSourceType");
        requireText(sourceEventType, "sourceEventType");
        requireText(sourceEntityType, "sourceEntityType");
        requireText(sourceOwnerRule, "sourceOwnerRule");
        if (targetValue <= 0) {
            throw new IllegalArgumentException("targetValue must be positive");
        }
        Objects.requireNonNull(targetUnit, "targetUnit");
        Objects.requireNonNull(repeatPolicy, "repeatPolicy");
        if (!repeatPolicy.isFinalPolicy()) {
            throw new IllegalArgumentException(
                    "repeatPolicy must be a final policy"
            );
        }
        Objects.requireNonNull(periodBoundary, "periodBoundary");
        Objects.requireNonNull(timezonePolicy, "timezonePolicy");
        if (timezonePolicy == QuestContentTimezonePolicy.PLAYER_PROFILE_TIMEZONE) {
            requireText(timezoneFallback, "timezoneFallback");
        } else if (timezoneFallback != null) {
            throw new IllegalArgumentException(
                    "timezoneFallback must be null when timezone is not applicable"
            );
        }
        Objects.requireNonNull(roleContextPolicy, "roleContextPolicy");
        allowedRoleTypes = Set.copyOf(
                Objects.requireNonNull(allowedRoleTypes, "allowedRoleTypes")
        );
        if (allowedRoleTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "allowedRoleTypes must not be empty"
            );
        }
        requireText(rewardProfileCode, "rewardProfileCode");
        if (manualCheckRequiresMemo && !manualCheckAllowed) {
            throw new IllegalArgumentException(
                    "manualCheckRequiresMemo requires manualCheckAllowed"
            );
        }
        if (autoComplete == manualCheckAllowed) {
            throw new IllegalArgumentException(
                    "autoComplete and manualCheckAllowed must be opposite"
            );
        }
        if (sortOrder <= 0) {
            throw new IllegalArgumentException("sortOrder must be positive");
        }
        requireText(iconKey, "iconKey");
        requireText(colorToken, "colorToken");
        requireText(resultCopyKey, "resultCopyKey");
        requireText(emptyStateCopyKey, "emptyStateCopyKey");
        notes = List.copyOf(Objects.requireNonNull(notes, "notes"));
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
