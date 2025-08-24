package online.lifeasgame.skill.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "skill_edges",
        uniqueConstraints = @UniqueConstraint(name = "uq_edge", columnNames = {"from_skill_id", "to_skill_id"})
)
public class SkillEdge extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_skill_id", nullable = false)
    private Long fromSkillId;

    @Column(name = "to_skill_id", nullable = false)
    private Long toSkillId;

    @Column(name = "req_level", nullable = false)
    private int reqLevel = 1;

    @PrePersist
    @PreUpdate
    void validate(){
        Guard.check(!fromSkillId.equals(toSkillId), "edge from != to");
        Guard.minValue(reqLevel, 1, "reqLevel");
    }
}
