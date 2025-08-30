package online.lifeasgame.economy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;


@Entity
@AggregateRoot
@Table(
        name = "price_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uq_item_time", columnNames = {"item_id", "taken_at"}),
        indexes = @Index(name = "idx_snapshot_item_time", columnList = "item_id,taken_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceSnapshot extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "avg_price", nullable = false)
    private Long avgPrice;

    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;

    @Column(name = "taken_at", nullable = false)
    private Instant takenAt;

    private PriceSnapshot(Long itemId, long avgPrice, int sampleSize, Instant takenAt) {
        this.itemId = Guard.notNull(itemId, "itemId");
        this.avgPrice = Guard.minValue(avgPrice, 0, "avgPrice");
        this.sampleSize = Guard.minValue(sampleSize, 0, "sampleSize");
        this.takenAt = Guard.notNull(takenAt, "takenAt");
    }

    public static PriceSnapshot of(Long itemId, long avgPrice, int sample, Instant at) {
        return new PriceSnapshot(itemId, avgPrice, sample, at);
    }
}
