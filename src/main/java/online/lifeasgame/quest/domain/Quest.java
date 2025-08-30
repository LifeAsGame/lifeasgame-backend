package online.lifeasgame.quest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Entity
@AggregateRoot
@Table(name = "quests")
public class Quest extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public QuestTarget target() {
        return target;
    }
}
