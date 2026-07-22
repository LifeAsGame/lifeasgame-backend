package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Experience {

    @Column(name = "exp", nullable = false)
    private long value;

    private Experience(long v) {
        if (v < 0) {
            throw new DomainException(PlayerError.PLAYER_EXP_MUST_NOT_BE_NEGATIVE);
        }
        this.value = v;
    }

    public static Experience of(long v) {
        return new Experience(v);
    }

    public long value() {
        return value;
    }

    public Experience plus(long delta) {
        if (delta < 0) {
            throw new DomainException(PlayerError.PLAYER_EXP_MUST_NOT_BE_NEGATIVE);
        }
        try {
            return new Experience(Math.addExact(value, delta));
        } catch (ArithmeticException exception) {
            throw new DomainException(PlayerError.PLAYER_EXP_OVERFLOW, null, exception);
        }
    }
}
