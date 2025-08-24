package online.lifeasgame.skill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkillCode {

    @Column(name = "code", length = 60, nullable = false, unique = true)
    private String value;

    private SkillCode(String raw){
        var v = raw == null ? "" : raw.trim().toUpperCase();
        Guard.check(v.isEmpty() || v.length() > 60, "invalid skill code");
        this.value = v;
    }

    public static SkillCode of(String raw) {

        return new SkillCode(raw);
    }

    public String value() {
        return value;
    }
}
