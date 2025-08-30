package online.lifeasgame.lifelog.domain;


import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "collection_items",
        indexes = @Index(name = "idx_collect_acquired", columnList = "player_id,acquired_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionItem extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CollectionCategory category;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "acquired_at", nullable = false)
    private Instant acquiredAt = Instant.now();

    @Column(length = 80)
    private String source;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "collection_item_tags",
            joinColumns = @JoinColumn(name = "collection_item_id"),
            uniqueConstraints = @UniqueConstraint(name = "uq_collect_item_tag", columnNames = {"collection_item_id", "tag"})
    )
    private Set<Tag> tags = new HashSet<>();

    private CollectionItem(Long playerId, CollectionCategory category, String name, Instant acquiredAt) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.category = Guard.notNull(category, "category");
        Guard.notBlank(name, "name");
        Guard.inRange(name.length(), 1, 120, "name");
        this.name = name;
        this.acquiredAt = Guard.notNull(acquiredAt, "acquiredAt");
    }

    public static CollectionItem acquire(Long playerId, CollectionCategory category, String name, Instant acquiredAt) {
        return new CollectionItem(playerId, category, name, acquiredAt == null ? Instant.now() : acquiredAt);
    }

    public void describe(String description, String source, String imageUrl) {
        this.description = (description == null) ? null : Guard.maxLength(description.strip(), 200, "description");
        this.source = (source == null) ? null : Guard.maxLength(source.strip(), 80, "source");
        this.imageUrl = (imageUrl == null) ? null : Guard.maxLength(imageUrl.strip(), 300, "imageUrl");
    }

    public void addTag(String t) { this.tags.add(Tag.of(t)); }
    public void removeTag(String t) { this.tags.remove(Tag.of(t)); }
}
