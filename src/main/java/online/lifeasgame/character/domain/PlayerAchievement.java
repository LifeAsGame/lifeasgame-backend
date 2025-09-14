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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "player_achievements",
        uniqueConstraints = @UniqueConstraint(name = "uq_player_achv", columnNames = {"player_id", "achievement_id"})
)
public class PlayerAchievement extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "achievement_id", nullable = false)
    private Long achievementId;

    @Column(name = "acquired_at", nullable = false)
    private Instant acquiredAt;

    public PlayerAchievement(Long playerId, Long achievementId) {
        this.playerId = playerId;
        this.achievementId = achievementId;
        this.acquiredAt = Instant.now();
    }

    public static PlayerAchievement create(Long playerId, Long achievementId) {
        return new PlayerAchievement(playerId, achievementId);
    }
}
