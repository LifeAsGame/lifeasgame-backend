package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class PartyCode {
    @Column(name = "code_value", nullable = false, length = 32, unique = true)
    private String value;

    public static PartyCode of(String code) {
        Guard.notBlank(code, "code");
        return new PartyCode(code.trim().toUpperCase());
    }

    private PartyCode(String value) {
        this.value = value;
    }
}
