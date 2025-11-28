package online.lifeasgame.quest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

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
