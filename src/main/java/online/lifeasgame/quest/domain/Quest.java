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
    @Column(name = "repeat_rule", length = 20)
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
            QuestTitle title,
            String descriptionMd,
            QuestTarget target,
            QuestReward reward,
            int definitionVersion,
            RewardProfileRef rewardProfileRef,
            QuestRepeatRule repeatRule,
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
        this.definitionVersion = definitionVersion;
        this.code = Guard.notBlank(code, "code").trim();
        this.category = Guard.notNull(category, "category");
        this.title = Guard.notNull(title, "title");
        this.descriptionMd = descriptionMd == null ? null : descriptionMd.trim();
        this.target = Guard.notNull(target, "target");
        this.reward = rewardProfileRef == null
                ? Guard.notNull(reward, "reward")
                : QuestReward.empty();
        this.rewardProfileRef = rewardProfileRef;
        this.repeatRule = repeatRule == null ? QuestRepeatRule.NONE : repeatRule;
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
                title,
                descriptionMd,
                target,
                reward,
                1,
                null,
                repeatRule,
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
                title,
                descriptionMd,
                target,
                null,
                definitionVersion,
                rewardProfileRef,
                repeatRule,
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
        validateUpdate(nextDefinitionVersion, questReward, nextRewardProfileRef);
        boolean changed = false;

        if (questTarget != null && !questTarget.equals(target)) {
            changeTarget(questTarget);
            changed = true;
        }
        if (questReward != null && !questReward.equals(reward)) {
            changeReward(questReward);
            changed = true;
        }
        if (questRepeatRule != null && questRepeatRule != repeatRule) {
            changeRepeatRule(questRepeatRule);
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
            RewardProfileRef nextRewardProfileRef
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
        this.repeatRule = repeatRule == null ? QuestRepeatRule.NONE : repeatRule;
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

    public String rewardProfileCodeOrNull() {
        return rewardProfileRef == null ? null : rewardProfileRef.code();
    }

    public String getCode() {
        return code;
    }
}
