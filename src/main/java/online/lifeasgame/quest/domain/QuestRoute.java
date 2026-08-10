package online.lifeasgame.quest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.quest.domain.error.QuestError;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
@AggregateRoot
@Table(name = "quest_routes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestRoute extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80, nullable = false, unique = true)
    private String code;

    @Column(name = "definition_version", nullable = false)
    private int definitionVersion;

    @Column(length = 120, nullable = false)
    private String title;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "primary_role_template_code", length = 80)
    private String primaryRoleTemplateCode;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "route_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    @OrderBy("stepOrder ASC")
    private List<QuestRouteStep> steps = new ArrayList<>();

    private QuestRoute(
            String code,
            int definitionVersion,
            String title,
            String description,
            String primaryRoleTemplateCode,
            List<QuestRouteStep> steps
    ) {
        this.code = normalize(code, 80);
        this.definitionVersion = definitionVersion;
        this.title = normalize(title, 120);
        this.description = normalizeNullable(description);
        this.primaryRoleTemplateCode = normalizeNullable(
                primaryRoleTemplateCode,
                80
        );
        this.steps = new ArrayList<>(steps == null ? List.of() : steps);
        validateDefinition();
    }

    public static QuestRoute define(
            String code,
            int definitionVersion,
            String title,
            String description,
            String primaryRoleTemplateCode,
            List<QuestRouteStep> steps
    ) {
        return new QuestRoute(
                code,
                definitionVersion,
                title,
                description,
                primaryRoleTemplateCode,
                steps
        );
    }

    public QuestRouteStep firstStep() {
        validateDefinition();
        return steps.getFirst();
    }

    public QuestRouteStep getStep(Long stepId) {
        return steps.stream()
                .filter(step -> step.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new DomainException(QuestError.ROUTE_STEP_NOT_FOUND));
    }

    public QuestRouteStep nextStep(Long currentStepId) {
        for (int index = 0; index < steps.size(); index++) {
            if (steps.get(index).getId().equals(currentStepId)) {
                return index + 1 < steps.size() ? steps.get(index + 1) : null;
            }
        }
        throw new DomainException(QuestError.ROUTE_STEP_NOT_FOUND);
    }

    public void validateDefinition() {
        if (definitionVersion < 1 || steps.isEmpty()) throw invalidDefinition();
        Set<String> codes = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        for (int index = 0; index < steps.size(); index++) {
            QuestRouteStep step = steps.get(index);
            step.validateDefinition();
            if (step.getStepOrder() != index + 1
                    || !codes.add(step.getStepCode())
                    || !orders.add(step.getStepOrder())) {
                throw invalidDefinition();
            }
        }
    }

    private static String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) throw invalidDefinition();
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw invalidDefinition();
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeNullable(String value, int maxLength) {
        String normalized = normalizeNullable(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw invalidDefinition();
        }
        return normalized;
    }

    private static DomainException invalidDefinition() {
        return new DomainException(QuestError.ROUTE_DEFINITION_INVALID);
    }
}
