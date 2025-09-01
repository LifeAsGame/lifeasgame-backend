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
public class HashedPassword {

    @Column(name = "password_hash", nullable = false)
    private String value;

    private HashedPassword(String value) {
        Guard.notBlank(value, "hashedPassword");
        this.value = value;
    }

    public static HashedPassword of(String value) {
        return new HashedPassword(value);
    }
}
