package online.lifeasgame.lifelog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@AggregateRoot
@Entity
@Table(
        name = "collection_logs",
        indexes = {
                @Index(name = "idx_collection_player", columnList = "player_id"),
                @Index(name = "idx_collection_category", columnList = "category")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionLog extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private CollectionCategory category;

    @Embedded
    private Title title;

    @Embedded
    private Quantity quantity;

    @Column(name = "condition_note", length = 100)
    private String conditionNote; // 새상품/중고/미개봉 등 텍스트

    @Column(name = "acquired_from", length = 100)
    private String acquiredFrom; // 구매처/출처

    @Embedded
    private CollectionTags tags;

    private CollectionLog(
            Long playerId,
            CollectionCategory category,
            Title title,
            Quantity quantity,
            String conditionNote,
            String acquiredFrom,
            CollectionTags tags
    ) {
        Guard.notNull(playerId, "playerId");
        Guard.notNull(category, "category");
        Guard.notNull(title, "title");
        Guard.notNull(quantity, "quantity");
        this.playerId = playerId;
        this.category = category;
        this.title = title;
        this.quantity = quantity;
        this.conditionNote = (conditionNote == null || conditionNote.isBlank()) ? null : conditionNote.trim();
        this.acquiredFrom = (acquiredFrom == null || acquiredFrom.isBlank()) ? null : acquiredFrom.trim();
        this.tags = (tags == null) ? CollectionTags.of(null) : tags;
    }

    public static CollectionLog create(
            Long playerId,
            CollectionCategory category,
            Title title,
            Quantity quantity,
            String conditionNote,
            String acquiredFrom,
            CollectionTags tags
    ) {
        return new CollectionLog(playerId, category, title, quantity, conditionNote, acquiredFrom, tags);
    }

    // 변경감지 행위
    public void changeQuantity(Integer value) {
        this.quantity = Quantity.of(value);
    }

    public void changeCondition(String note) {
        this.conditionNote = (note == null || note.isBlank()) ? null : note.trim();
    }

    public void changeAcquiredFrom(String from) {
        this.acquiredFrom = (from == null || from.isBlank()) ? null : from.trim();
    }
}
