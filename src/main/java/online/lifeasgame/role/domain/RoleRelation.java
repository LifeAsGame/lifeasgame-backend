package online.lifeasgame.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.role.domain.error.RoleError;

@Entity
@AggregateRoot
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "role_relations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_role_relation_role_person",
                columnNames = {"role_id", "person_id"}
        ),
        indexes = {
                @Index(
                        name = "idx_role_relation_player_role_status",
                        columnList = "player_id,role_id,status,id"
                ),
                @Index(
                        name = "idx_role_relation_player_person_status",
                        columnList = "player_id,person_id,status,id"
                )
        }
)
public class RoleRelation extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private Long roleId;

    @Column(name = "person_id", nullable = false, updatable = false)
    private Long personId;

    @Embedded
    private RoleRelationType relationType;

    @Column(name = "role_notes", columnDefinition = "text")
    private String roleNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleRelationStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    private RoleRelation(
            Long playerId,
            Long roleId,
            Long personId,
            RoleRelationType relationType,
            String roleNotes
    ) {
        this.playerId = positive(playerId, "playerId");
        this.roleId = positive(roleId, "roleId");
        this.personId = positive(personId, "personId");
        this.relationType = Guard.notNull(relationType, "relationType");
        this.roleNotes = optionalNotes(roleNotes);
        this.status = RoleRelationStatus.ACTIVE;
    }

    public static RoleRelation create(
            Long playerId,
            Long roleId,
            Long personId,
            RoleRelationType relationType,
            String roleNotes
    ) {
        return new RoleRelation(
                playerId,
                roleId,
                personId,
                relationType,
                roleNotes
        );
    }

    public void update(RoleRelationType relationType, String roleNotes) {
        if (status == RoleRelationStatus.ARCHIVED) {
            throw new DomainException(RoleError.ROLE_RELATION_ARCHIVED);
        }
        apply(relationType, roleNotes);
    }

    public void archive() {
        status = RoleRelationStatus.ARCHIVED;
    }

    public void reactivate(RoleRelationType relationType, String roleNotes) {
        apply(relationType, roleNotes);
        status = RoleRelationStatus.ACTIVE;
    }

    private void apply(RoleRelationType relationType, String roleNotes) {
        this.relationType = Guard.notNull(relationType, "relationType");
        this.roleNotes = optionalNotes(roleNotes);
    }

    private static Long positive(Long value, String name) {
        return Guard.minValue(Guard.notNull(value, name), 1, name);
    }

    private static String optionalNotes(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
