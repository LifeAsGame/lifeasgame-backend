package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairKey {

    @Column(name = "low_id", nullable = false)
    private Long low;

    @Column(name = "high_id", nullable = false)
    private Long high;

    private PairKey(Long low, Long high) {
        this.low = low; this.high = high;
    }

    public static PairKey of(Long a, Long b) {
        Guard.notNull(a, "pair key");
        Guard.notNull(b, "pair key");
        Guard.check(a.equals(b), "pair key should be the same");
        return (a < b) ? new PairKey(a, b) : new PairKey(b, a);
    }

    public Long low(){ return low; }
    public Long high(){ return high; }
}
