package online.lifeasgame.skill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkillName {

    @Column(name = "name", length = 80, nullable = false)
    private String value;

    private SkillName(String raw) {
        var v = raw == null ? "" : raw.trim();
        Guard.inRange(v.length(), 2, 80, "name");
        this.value = v;
    }

    public static SkillName of(String raw) {
        return new SkillName(raw);
    }

    public String value() {
        return value;
    }
}
