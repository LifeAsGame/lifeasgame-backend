package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Nickname {

    @Column(name = "nickname", length = 20, nullable = false)
    private String nickname;

    private Nickname(String nickname) {
        this.nickname = nickname;
    }

    public static Nickname of(String nickname) {
        Objects.requireNonNull(nickname);
        var v = nickname.trim();
        if (v.length() < 2 || v.length() > 20) throw new IllegalArgumentException("username length 2~20");

        return new Nickname(nickname);
    }
}
