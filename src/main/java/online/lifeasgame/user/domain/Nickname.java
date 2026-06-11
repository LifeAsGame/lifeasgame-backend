package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Nickname {

    @Column(name = "nickname", length = 20, nullable = false)
    private String value;

    private Nickname(String raw) {
        String value = Guard.notBlank(raw, "nickname");
        Guard.check(value.length() >= 2 && value.length() <= 20, "nickname 2~20");
        this.value = value;
    }

    public static Nickname of(String value) {
        return new Nickname(value);
    }
}
