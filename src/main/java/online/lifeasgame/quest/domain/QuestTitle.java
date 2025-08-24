package online.lifeasgame.quest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestTitle {

    @Column(name = "title", length = 120, nullable = false)
    private String value;

    private QuestTitle(String raw) {
        String v = Guard.notBlank(raw, "title").trim();
        Guard.inRange(v.length(), 2, 120, "title");
        this.value = v;
    }

    public static QuestTitle of(String raw) {
        return new QuestTitle(raw);
    }
    public String value() {
        return value;
    }
}
