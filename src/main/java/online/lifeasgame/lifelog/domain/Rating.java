package online.lifeasgame.lifelog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rating {

    @Column(name = "rating")
    private Integer value;

    private Rating(Integer v) {
        if (v != null) Guard.inRange(v, 1, 10, "rating (1~10)");
        this.value = v;
    }


    public static Rating of(Integer v) { return new Rating(v); }
    public Optional<Integer> value() { return Optional.ofNullable(value); }
}
