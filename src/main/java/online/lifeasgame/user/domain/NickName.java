package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NickName {

    @Column(name = "nickname", length = 20, nullable = false)
    private String nickname;

    private NickName(String nickname) {
        this.nickname = nickname;
    }

    public static NickName of(String nickname) {
        Objects.requireNonNull(nickname);
        var v = nickname.trim();
        if (v.length() < 2 || v.length() > 20) throw new IllegalArgumentException("username length 2~20");

        return new NickName(nickname);
    }
}
