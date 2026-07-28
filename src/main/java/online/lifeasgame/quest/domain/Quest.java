package online.lifeasgame.quest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@AggregateRoot
@Table(name = "quests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quest extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_version", nullable = false)
    private int definitionVersion = 1;

    @Column(name = "code", length = 80, nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private QuestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "semantic_category", length = 20)
    private QuestSemanticCategory semanticCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_source", length = 30)
    private QuestProgressSource progressSource;

    @Embedded
    private QuestRoleTemplateRef roleTemplateRef;

    @Embedded
    private QuestTitle title;

    @Lob
    @Column(name = "description_md")
    private String descriptionMd;

    @Embedded
    private QuestTarget target;

    @Embedded
    private QuestReward reward;

    @Embedded
    private RewardProfileRef rewardProfileRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_rule", length = 20, nullable = false)
    private QuestRepeatRule repeatRule = QuestRepeatRule.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_policy", length = 20, nullable = false)
    private QuestCompletionPolicy completionPolicy = QuestCompletionPolicy.AUTO;

    @Column(name = "due_at")
    private Instant dueAt;

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Quest(
            String code,
            QuestCategory category,
            QuestSemanticCategory semanticCategory,
            QuestTitle title,
            String descriptionMd,
            QuestTarget target,
            QuestProgressSource progressSource,
            QuestReward reward,
            int definitionVersion,
            RewardProfileRef rewardProfileRef,
            QuestRepeatRule repeatRule,
            QuestRoleTemplateRef roleTemplateRef,
            QuestCompletionPolicy completionPolicy,
            Instant dueAt
    ) {
        validateDefinitionVersion(definitionVersion);
        if (rewardProfileRef == null && reward == null) {
            throw new DomainException(QuestError.QUEST_REWARD_PROFILE_CODE_REQUIRED);
        }
        if (rewardProfileRef != null && reward != null) {
            throw new DomainException(QuestError.QUEST_REWARD_CONTRACT_CONFLICT);
        }
        QuestRepeatRule normalizedRepeatRule =
                repeatRule == null ? QuestRepeatRule.NONE : repeatRule;
        validateCreationContract(
                semanticCategory,
                progressSource,
                normalizedRepeatRule,
                roleTemplateRef
        );
        this.definitionVersion = definitionVersion;
        this.code = Guard.notBlank(code, "code").trim();
        this.category = Guard.notNull(category, "category");
        this.semanticCategory = semanticCategory;
        this.title = Guard.notNull(title, "title");
        this.descriptionMd = descriptionMd == null ? null : descriptionMd.trim();
        this.target = Guard.notNull(target, "target");
        this.progressSource = progressSource;
        this.reward = rewardProfileRef == null
                ? Guard.notNull(reward, "reward")
                : QuestReward.empty();
        this.rewardProfileRef = rewardProfileRef;
        this.repeatRule = normalizedRepeatRule;
        this.roleTemplateRef = roleTemplateRef;
        this.completionPolicy = QuestCompletionPolicy.defaultIfNull(completionPolicy);
        this.dueAt = dueAt;
    }

    public static Quest create(
            String code,
            QuestCategory category,
            QuestTitle title,
            String descriptionMd,
            QuestTarget target,
            QuestReward reward,
            QuestRepeatRule repeatRule,
            Instant dueAt
    ) {
        return create(
                code,
                category,
                title,
                descriptionMd,
                target,
                reward,
                repeatRule,
                QuestCompletionPolicy.AUTO,
                dueAt
        );
    }

    public static Quest create(
            String code,
            QuestCategory category,
            QuestTitle title,
            String descriptionMd,
            QuestTarget target,
            QuestReward reward,
            QuestRepeatRule repeatRule,
            QuestCompletionPolicy completionPolicy,
            Instant dueAt
    ) {
        return new Quest(
                code,
                category,
                null,
                title,
                descriptionMd,
                target,
                null,
                reward,
                1,
                null,
                repeatRule,
                null,
                completionPolicy,
                dueAt
        );
    }

    public static Quest createDefinition(
            String code,
            int definitionVersion,
            QuestCategory category,
            QuestTitle title,
            String descriptionMd,
            QuestTarget target,
            RewardProfileRef rewardProfileRef,
            QuestRepeatRule repeatRule,
            QuestCompletionPolicy completionPolicy,
            Instant dueAt
    ) {
        return new Quest(
                code,
                category,
                null,
                title,
                descriptionMd,
                target,
                null,
                null,
                definitionVersion,
                rewardProfileRef,
                repeatRule,
                null,
                completionPolicy,
                dueAt
        );
    }

    public static Quest createDefinition(
            String code,
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
            QuestCompletionPolicy completionPolicy,
            Instant dueAt
    ) {
        validateFinalContract(
                semanticCategory,
                progressSource,
                repeatPolicy
        );
        return new Quest(
                code,
                category,
                semanticCategory,
                title,
                descriptionMd,
                target,
                progressSource,
                null,
                definitionVersion,
                rewardProfileRef,
                repeatPolicy,
                roleTemplateRef,
                completionPolicy,
                dueAt
        );
    }

    public void updateDefinition(
            QuestTarget questTarget,
            QuestReward questReward,
            QuestRepeatRule questRepeatRule,
            Instant dueAt,
            Integer nextDefinitionVersion,
            RewardProfileRef nextRewardProfileRef
    ) {
        updateDefinition(
                questTarget,
                questReward,
                questRepeatRule,
                dueAt,
                nextDefinitionVersion,
                nextRewardProfileRef,
                null,
                null,
                null,
                null
        );
    }

    public void updateDefinition(
            QuestTarget questTarget,
            QuestReward questReward,
            QuestRepeatRule questRepeatRule,
            Instant dueAt,
            Integer nextDefinitionVersion,
            RewardProfileRef nextRewardProfileRef,
            QuestSemanticCategory nextSemanticCategory,
            QuestProgressSource nextProgressSource,
            QuestRepeatRule nextRepeatPolicy,
            QuestRoleTemplateRef nextRoleTemplateRef
    ) {
        QuestRepeatRule requestedRepeatRule = resolveRequestedRepeatRule(
                questRepeatRule,
                nextRepeatPolicy
        );
        validateUpdate(
                nextDefinitionVersion,
                questReward,
                nextRewardProfileRef,
                requestedRepeatRule,
                nextSemanticCategory,
                nextProgressSource,
                nextRepeatPolicy,
                nextRoleTemplateRef
        );
        boolean changed = false;

        if (questTarget != null && !questTarget.equals(target)) {
            changeTarget(questTarget);
            changed = true;
        }
        if (questReward != null && !questReward.equals(reward)) {
            changeReward(questReward);
            changed = true;
        }
        if (requestedRepeatRule != null
                && requestedRepeatRule != repeatRule) {
            repeatRule = requestedRepeatRule;
            changed = true;
        }
        if (dueAt != null && !Objects.equals(dueAt, this.dueAt)) {
            reschedule(dueAt);
            changed = true;
        }
        if (nextDefinitionVersion != null
                && nextDefinitionVersion != definitionVersion) {
            definitionVersion = nextDefinitionVersion;
            changed = true;
        }
        if (nextRewardProfileRef != null
                && !nextRewardProfileRef.equals(rewardProfileRef)) {
            rewardProfileRef = nextRewardProfileRef;
            reward = QuestReward.empty();
            changed = true;
        }
        if (nextSemanticCategory != null
                && nextSemanticCategory != semanticCategory) {
            semanticCategory = nextSemanticCategory;
            changed = true;
        }
        if (nextProgressSource != null
                && nextProgressSource != progressSource) {
            progressSource = nextProgressSource;
            changed = true;
        }
        if (nextRoleTemplateRef != null
                && !nextRoleTemplateRef.equals(roleTemplateRef)) {
            roleTemplateRef = nextRoleTemplateRef;
            changed = true;
        }

        if (changed) recordUpdatedEvent();
    }

    public void updateDefinition(
            QuestTarget questTarget,
            QuestReward questReward,
            QuestRepeatRule questRepeatRule,
            Instant dueAt
    ) {
        updateDefinition(
                questTarget,
                questReward,
                questRepeatRule,
                dueAt,
                null,
                null
        );
    }

    private void validateUpdate(
            Integer nextDefinitionVersion,
            QuestReward questReward,
            RewardProfileRef nextRewardProfileRef,
            QuestRepeatRule requestedRepeatRule,
            QuestSemanticCategory nextSemanticCategory,
            QuestProgressSource nextProgressSource,
            QuestRepeatRule nextRepeatPolicy,
            QuestRoleTemplateRef nextRoleTemplateRef
    ) {
        if (nextDefinitionVersion != null) {
            validateDefinitionVersion(nextDefinitionVersion);
            if (nextDefinitionVersion < definitionVersion) {
                throw new DomainException(
                        QuestError.QUEST_DEFINITION_VERSION_DECREASE_NOT_ALLOWED
                );
            }
        }
        if (questReward != null
                && (nextRewardProfileRef != null || usesRewardProfile())) {
            throw new DomainException(QuestError.QUEST_REWARD_CONTRACT_CONFLICT);
        }
        QuestSemanticCategory candidateSemanticCategory =
                nextSemanticCategory == null
                        ? semanticCategory
                        : nextSemanticCategory;
        QuestProgressSource candidateProgressSource =
                nextProgressSource == null
                        ? progressSource
                        : nextProgressSource;
        QuestRepeatRule candidateRepeatRule =
                requestedRepeatRule == null
                        ? repeatRule
                        : requestedRepeatRule;
        QuestRoleTemplateRef candidateRoleTemplateRef =
                nextRoleTemplateRef == null
                        ? roleTemplateRef
                        : nextRoleTemplateRef;
        boolean requestsFinalContract = isFinalContract()
                || nextSemanticCategory != null
                || nextProgressSource != null
                || nextRepeatPolicy != null
                || nextRoleTemplateRef != null;
        if (requestsFinalContract) {
            if (nextRewardProfileRef == null && !usesRewardProfile()) {
                throw new DomainException(
                        QuestError.QUEST_REWARD_PROFILE_CODE_REQUIRED
                );
            }
            validateFinalContract(
                    candidateSemanticCategory,
                    candidateProgressSource,
                    candidateRepeatRule
            );
        } else if (candidateRepeatRule == QuestRepeatRule.ONCE) {
            validateFinalContract(
                    candidateSemanticCategory,
                    candidateProgressSource,
                    candidateRepeatRule
            );
        }
        if (candidateRoleTemplateRef != null && !requestsFinalContract) {
            throw new DomainException(
                    QuestError.QUEST_SEMANTIC_CATEGORY_REQUIRED
            );
        }
    }

    private QuestRepeatRule resolveRequestedRepeatRule(
            QuestRepeatRule questRepeatRule,
            QuestRepeatRule nextRepeatPolicy
    ) {
        if (questRepeatRule != null
                && nextRepeatPolicy != null
                && questRepeatRule != nextRepeatPolicy) {
            throw new DomainException(
                    QuestError.QUEST_REPEAT_CONTRACT_CONFLICT
            );
        }
        return nextRepeatPolicy == null ? questRepeatRule : nextRepeatPolicy;
    }

    private static void validateCreationContract(
            QuestSemanticCategory semanticCategory,
            QuestProgressSource progressSource,
            QuestRepeatRule repeatRule,
            QuestRoleTemplateRef roleTemplateRef
    ) {
        boolean finalContractRequested = semanticCategory != null
                || progressSource != null
                || roleTemplateRef != null
                || repeatRule == QuestRepeatRule.ONCE;
        if (finalContractRequested) {
            validateFinalContract(
                    semanticCategory,
                    progressSource,
                    repeatRule
            );
        }
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

    private static void validateDefinitionVersion(int definitionVersion) {
        if (definitionVersion < 1) {
            throw new DomainException(QuestError.QUEST_DEFINITION_VERSION_INVALID);
        }
    }

    private void recordUpdatedEvent() {
        recordEvent(QuestEvent.snapshot(QuestEventType.QUEST_UPDATED, this, "quest:" + code + ":updated"));
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(domainEvents);
        domainEvents.clear();
        return copy;
    }

    private void recordEvent(DomainEvent event) {
        if (event != null) domainEvents.add(event);
    }

    public void changeTarget(QuestTarget target) {
        this.target = Guard.notNull(target, "target");
    }

    public void changeReward(QuestReward reward) {
        if (usesRewardProfile()) {
            throw new DomainException(QuestError.QUEST_REWARD_CONTRACT_CONFLICT);
        }
        this.reward = Guard.notNull(reward, "reward");
    }

    public void changeRepeatRule(QuestRepeatRule repeatRule) {
        QuestRepeatRule normalized =
                repeatRule == null ? QuestRepeatRule.NONE : repeatRule;
        if (isFinalContract() && !normalized.isFinalPolicy()) {
            throw new DomainException(QuestError.INVALID_QUEST_REPEAT_POLICY);
        }
        if (!isFinalContract() && normalized == QuestRepeatRule.ONCE) {
            throw new DomainException(
                    QuestError.QUEST_SEMANTIC_CATEGORY_REQUIRED
            );
        }
        this.repeatRule = normalized;
    }

    public void reschedule(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public QuestTarget target() {
        return target;
    }

    public boolean isAutoCompletion() {
        return completionPolicy == QuestCompletionPolicy.AUTO;
    }

    public boolean requiresUserConfirmation() {
        return completionPolicy == QuestCompletionPolicy.USER_CONFIRM;
    }

    public boolean usesRewardProfile() {
        return rewardProfileRef != null;
    }

    public boolean isLegacyInlineReward() {
        return !usesRewardProfile();
    }

    public boolean isFinalContract() {
        return semanticCategory != null
                && progressSource != null
                && repeatRule != null
                && repeatRule.isFinalPolicy();
    }

    public QuestRepeatRule repeatPolicyOrNull() {
        return isFinalContract() ? repeatRule : null;
    }

    public String roleTemplateCodeOrNull() {
        return roleTemplateRef == null ? null : roleTemplateRef.code();
    }

    public String rewardProfileCodeOrNull() {
        return rewardProfileRef == null ? null : rewardProfileRef.code();
    }

    public String getCode() {
        return code;
    }
}
