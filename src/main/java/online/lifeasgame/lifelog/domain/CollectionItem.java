package online.lifeasgame.lifelog.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;


@Getter
@Entity
@AggregateRoot
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "lifelog_collections", indexes = {
        @Index(name = "idx_col_player", columnList = "player_id"), @Index(name = "idx_col_kind", columnList = "kind")
}
)
public class CollectionItem extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private CollectionKind kind;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "series", length = 200)
    private String series; // e.g., set/series name for cards/figures

    @Column(name = "owned", nullable = false)
    private boolean owned; // quickly check if already owned

    @Column(name = "tags", length = 200)
    private String tags; // simple CSV tags for quick search (alt: separate table later)

    protected CollectionItem(
            Long playerId,
            CollectionKind kind,
            String name,
            String series,
            boolean owned,
            String tags
    ) {
        this.playerId = playerId;
        this.kind = kind;
        this.name = name;
        this.series = series;
        this.owned = owned;
        this.tags = tags;
    }

    public static CollectionItem create(
            Long playerId,
            CollectionKind kind,
            String name,
            String series,
            boolean owned,
            String tags
    ) {
        if (playerId == null || playerId <= 0) {
            throw new IllegalArgumentException("playerId invalid");
        }

        if (kind == null) {
            throw new IllegalArgumentException("kind required");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name required");
        }
        return new CollectionItem(
                playerId,
                kind,
                name.trim(),
                (series == null ? null : series.trim()),
                owned,
                (tags == null ? null : tags.trim())
        );
    }

    public void markOwned(boolean owned) {
        this.owned = owned;
    }

    public void retag(String tags) {
        this.tags = (tags == null ? null : tags.trim());
    }
}
