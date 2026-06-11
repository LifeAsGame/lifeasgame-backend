package online.lifeasgame.lifelog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rating {

    @Column(name = "rating_score", nullable = false)
    private Double score;

    private Rating(Double score) {
        Guard.notNull(score, "score");
        Guard.inRange(score, 0.0, 5.0, "score");
        this.score = score;
    }

    public static Rating of(Double score) {
        return new Rating(score);
    }

    public static Rating unrated() {
        return new Rating(0.0);
    }

    public Double score() {
        return score;
    }

    public boolean isRated() {
        return score > 0.0;
    }
}
