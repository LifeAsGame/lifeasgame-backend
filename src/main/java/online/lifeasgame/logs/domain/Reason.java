package online.lifeasgame.logs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Reason {

    @Column(name = "reason", length = 40, nullable = false)
    private String value;

    private Reason(String v) {
        String s = Guard.notBlank(v, "reason").trim();
        Guard.inRange(s.length(), 1, 40, "reason length");
        this.value = s;
    }

    public static Reason of(String v) {
        return new Reason(v);
    }

    public String value() {
        return value;
    }
}
