package online.lifeasgame.quest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestTarget {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30, nullable = false)
    private QuestTargetType type;

    @Column(name = "target_value", nullable = false)
    private int value;

    private QuestTarget(QuestTargetType type, int value) {
        Guard.notNull(type, "targetType");
        Guard.minValue(value, 1, "targetValue");
        this.type = type;
        this.value = value;
    }

    public static QuestTarget of(QuestTargetType type, int value) {
        return new QuestTarget(type, value);
    }

    public boolean reachedBy(int progress) {
        return progress >= value;
    }

    public QuestTargetType type() {
        return type;
    }

    public int value() {
        return value;
    }
}
