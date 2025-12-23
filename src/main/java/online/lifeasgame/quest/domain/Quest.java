package online.lifeasgame.quest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@AggregateRoot
@Table(name = "quests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quest extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_rule", length = 20)
    private QuestRepeatRule repeatRule = QuestRepeatRule.NONE;

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
            QuestRepeatRule repeatRule,
            Instant dueAt
    ) {
        this.code = Guard.notBlank(code, "code").trim();
        this.category = Guard.notNull(category, "category");
        this.title = Guard.notNull(title, "title");
        this.descriptionMd = descriptionMd == null ? null : descriptionMd.trim();
        this.target = Guard.notNull(target, "target");
        this.reward = Guard.notNull(reward, "reward");
        this.repeatRule = repeatRule == null ? QuestRepeatRule.NONE : repeatRule;
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
        return new Quest(code, category, title, descriptionMd, target, reward, repeatRule, dueAt);
    }

    public void updateDefinition(
            QuestTarget questTarget,
            QuestReward questReward,
            QuestRepeatRule questRepeatRule,
            Instant dueAt
    ) {
        boolean changed = false;

        if (questTarget != null) {
            changeTarget(questTarget);
            changed = true;
        }
        if (questReward != null) {
            changeReward(questReward);
            changed = true;
        }
        if (questRepeatRule != null) {
            changeRepeatRule(questRepeatRule);
            changed = true;
        }
        if (dueAt != null) {
            reschedule(dueAt);
            changed = true;
        }

        if (changed) recordUpdatedEvent();
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

    public String getCode() {
        return code;
    }
}
