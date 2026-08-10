package online.lifeasgame.quest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;

import java.util.Objects;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestRouteStepQuest {

    @Column(name = "quest_id", nullable = false)
    private Long questId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", length = 20, nullable = false)
    private QuestRouteQuestRequirementType requirementType;

    private QuestRouteStepQuest(
            Long questId,
            QuestRouteQuestRequirementType requirementType
    ) {
        if (questId == null || questId <= 0 || requirementType == null) {
            throw invalidDefinition();
        }
        this.questId = questId;
        this.requirementType = requirementType;
    }

    public static QuestRouteStepQuest required(Long questId) {
        return new QuestRouteStepQuest(
                questId,
                QuestRouteQuestRequirementType.REQUIRED
        );
    }

    public static QuestRouteStepQuest optional(Long questId) {
        return new QuestRouteStepQuest(
                questId,
                QuestRouteQuestRequirementType.OPTIONAL
        );
    }

    public boolean isRequired() {
        return requirementType == QuestRouteQuestRequirementType.REQUIRED;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof QuestRouteStepQuest that)) return false;
        return Objects.equals(questId, that.questId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questId);
    }

    private static DomainException invalidDefinition() {
        return new DomainException(QuestError.ROUTE_DEFINITION_INVALID);
    }
}
