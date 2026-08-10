package online.lifeasgame.quest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(
        name = "quest_route_steps",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_quest_route_step_order",
                        columnNames = {"route_id", "step_order"}
                ),
                @UniqueConstraint(
                        name = "uq_quest_route_step_code",
                        columnNames = {"route_id", "step_code"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestRouteStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "step_code", length = 80, nullable = false)
    private String stepCode;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(length = 120, nullable = false)
    private String title;

    @Lob
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion_type", length = 40, nullable = false)
    private QuestRouteCriterionType criterionType;

    @Column(name = "required_evidence_count", nullable = false)
    private int requiredEvidenceCount;

    @Column(name = "user_advance_required", nullable = false)
    private boolean userAdvanceRequired;

    @Column(name = "retroactive_evidence_allowed", nullable = false)
    private boolean retroactiveEvidenceAllowed;

    @Column(name = "skip_allowed", nullable = false)
    private boolean skipAllowed;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "quest_route_step_quests",
            joinColumns = @JoinColumn(name = "step_id")
    )
    private Set<QuestRouteStepQuest> questLinks = new LinkedHashSet<>();

    private QuestRouteStep(
            String stepCode,
            int stepOrder,
            String title,
            String description,
            int requiredEvidenceCount,
            Set<QuestRouteStepQuest> questLinks
    ) {
        this.stepCode = normalize(stepCode, 80);
        this.stepOrder = stepOrder;
        this.title = normalize(title, 120);
        this.description = normalizeNullable(description);
        this.criterionType = QuestRouteCriterionType.QUEST_COMPLETION_SET;
        this.requiredEvidenceCount = requiredEvidenceCount;
        this.userAdvanceRequired = true;
        this.retroactiveEvidenceAllowed = true;
        this.skipAllowed = false;
        this.questLinks = new LinkedHashSet<>(questLinks == null ? Set.of() : questLinks);
        validateDefinition();
    }

    public static QuestRouteStep define(
            String stepCode,
            int stepOrder,
            String title,
            String description,
            int requiredEvidenceCount,
            Set<QuestRouteStepQuest> questLinks
    ) {
        return new QuestRouteStep(
                stepCode,
                stepOrder,
                title,
                description,
                requiredEvidenceCount,
                questLinks
        );
    }

    public Set<Long> requiredQuestIds() {
        return questLinks.stream()
                .filter(QuestRouteStepQuest::isRequired)
                .map(QuestRouteStepQuest::getQuestId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    void validateDefinition() {
        long requiredQuestCount = questLinks.stream()
                .filter(QuestRouteStepQuest::isRequired)
                .count();
        if (stepOrder < 1
                || criterionType != QuestRouteCriterionType.QUEST_COMPLETION_SET
                || requiredEvidenceCount < 1
                || requiredEvidenceCount > requiredQuestCount
                || !userAdvanceRequired
                || !retroactiveEvidenceAllowed
                || skipAllowed) {
            throw invalidDefinition();
        }
    }

    private static String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) throw invalidDefinition();
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw invalidDefinition();
        return normalized;
    }

    private static String normalizeNullable(String value) {
        return value == null ? null : value.trim();
    }

    private static DomainException invalidDefinition() {
        return new DomainException(QuestError.ROUTE_DEFINITION_INVALID);
    }
}
