package online.lifeasgame.skill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import online.lifeasgame.core.annotation.AggregateRoot;

@Entity
@AggregateRoot
@Table(
        name = "player_skills",
        uniqueConstraints = @UniqueConstraint(name = "pk_player_skill", columnNames = {"player_id", "skill_id"})
)
public class PlayerSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(nullable = false)
    private int level = 0;

    @Version
    private Long version;
}
