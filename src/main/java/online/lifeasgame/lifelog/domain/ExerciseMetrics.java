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
public class ExerciseMetrics {

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "calories")
    private Integer calories;

    private ExerciseMetrics(Integer durationMinutes, Double distanceKm, Integer calories) {
        Guard.notNull(durationMinutes, "durationMinutes");
        Guard.checkState(durationMinutes >= 1, "durationMinutes >= 1");
        if (distanceKm != null) {
            Guard.checkState(distanceKm >= 0, "distanceKm >= 0");
        }
        if (calories != null) {
            Guard.checkState(calories >= 0, "calories >= 0");
        }
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;
        this.calories = calories;
    }

    public static ExerciseMetrics of(Integer durationMinutes, Double distanceKm, Integer calories) {
        return new ExerciseMetrics(durationMinutes, distanceKm, calories);
    }

    public Integer durationMinutes() {
        return durationMinutes;
    }

    public Double distanceKm() {
        return distanceKm;
    }

    public Integer calories() {
        return calories;
    }
}
