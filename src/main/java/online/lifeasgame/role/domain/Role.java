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
        name = "roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_role_id_player",
                columnNames = {"id", "player_id"}
        ),
        indexes = @Index(
                name = "idx_role_player_status",
                columnList = "player_id,status,id"
        )
)
public class Role extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;

    @Embedded
    private RoleType roleType;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    private Role(Long playerId, RoleType roleType, String name, String description) {
        this.playerId = positive(playerId);
        this.roleType = Guard.notNull(roleType, "roleType");
        this.name = requiredName(name);
        this.description = optionalDescription(description);
        this.status = RoleStatus.ACTIVE;
    }

    public static Role create(
            Long playerId,
            RoleType roleType,
            String name,
            String description
    ) {
        return new Role(playerId, roleType, name, description);
    }

    public void update(RoleType roleType, String name, String description) {
        if (status == RoleStatus.ARCHIVED) {
            throw new DomainException(RoleError.ROLE_ARCHIVED);
        }
        this.roleType = Guard.notNull(roleType, "roleType");
        this.name = requiredName(name);
        this.description = optionalDescription(description);
    }

    public void archive() {
        status = RoleStatus.ARCHIVED;
    }

    private static Long positive(Long value) {
        return Guard.minValue(Guard.notNull(value, "playerId"), 1, "playerId");
    }

    private static String requiredName(String value) {
        if (value == null) {
            throw new DomainException(RoleError.INVALID_ROLE_NAME);
        }
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 60) {
            throw new DomainException(RoleError.INVALID_ROLE_NAME);
        }
        return normalized;
    }

    private static String optionalDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 500) {
            throw new DomainException(RoleError.INVALID_ROLE_DESCRIPTION);
        }
        return normalized;
    }
}
