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
public class EpisodeProgress {

    @Column(name = "progress_current", nullable = false)
    private Integer current;

    @Column(name = "progress_total", nullable = false)
    private Integer total;

    private EpisodeProgress(Integer current, Integer total) {
        Guard.notNull(current, "current");
        Guard.notNull(total, "total");
        Guard.minValue(current, 0, "current");
        Guard.minValue(total, 1, "total");
        Guard.check(current <= total, "current > total");
        this.current = current;
        this.total = total;
    }

    public static EpisodeProgress of(int current, int total) {
        return new EpisodeProgress(current, total);
    }

    public EpisodeProgress advance(int step) {
        Guard.minValue(step, 1, "step");
        int next = Math.min(current + step, total);
        return new EpisodeProgress(next, total);
    }

    public boolean completed() {
        return current.equals(total);
    }

    public int current() {
        return current;
    }

    public int total() {
        return total;
    }
}
