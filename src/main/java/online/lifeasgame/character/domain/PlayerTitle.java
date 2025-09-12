package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@AggregateRoot
@Table(
        name = "player_titles",
        uniqueConstraints = @UniqueConstraint(name = "uq_player_title", columnNames = {"player_id", "title_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerTitle extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "title_id", nullable = false)
    private Long titleId;

    @Column(name = "acquired_at", nullable = false)
    private Instant acquiredAt;

    public PlayerTitle(Long playerId, Long titleId) {
        this.playerId = playerId;
        this.titleId = titleId;
        this.acquiredAt = Instant.now();
    }

    public static PlayerTitle create(Long playerId, Long titleId) {
        return new PlayerTitle(playerId, titleId);
    }
}
