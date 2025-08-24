package online.lifeasgame.skill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

@Entity
@AggregateRoot
@Table(name = "player_skill_points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerSkillPoint extends AbstractTime {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "available_sp", nullable = false)
    private int availableSp = 0;

    @Version
    private Long version;

    private PlayerSkillPoint(Long playerId, int availableSp) {
        this.playerId = playerId;
        this.availableSp = availableSp;
    }

    public static PlayerSkillPoint of(Long playerId) {
        return new PlayerSkillPoint(playerId, 0);
    }

    public void grant(int amount) {
        Guard.minValue(amount, 0, "amount");
        this.availableSp += amount;
    }

    public void consume(int amount) {
        Guard.minValue(amount, 0, "amount");
        if (availableSp < amount) throw new IllegalStateException("not enough SP");
        this.availableSp -= amount;
    }
}
