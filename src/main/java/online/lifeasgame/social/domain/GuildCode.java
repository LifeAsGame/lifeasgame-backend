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
public class GuildCode {

    @Column(name = "code_value", nullable = false, length = 32, unique = true)
    private String value;

    public static GuildCode of(String code) {
        Guard.notBlank(code, "code");
        return new GuildCode(code.trim().toUpperCase());
    }

    private GuildCode(String value) {
        this.value = value;
    }
}
