package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Nickname {

    @Column(name = "nickname", length = 20, nullable = false)
    private String nickname;

    private Nickname(String raw) {
        String nickname = Guard.notBlank(raw, "nickname");
        Guard.check(nickname.length() >= 2 && nickname.length() <= 20, "nickname 2~20");
        this.nickname = nickname;
    }

    public static Nickname of(String nickname) {
        return new Nickname(nickname);
    }
}
