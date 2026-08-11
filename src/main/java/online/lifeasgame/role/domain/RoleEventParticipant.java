package online.lifeasgame.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Entity
@Getter
@Table(
        name = "role_event_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_role_event_participant",
                columnNames = {
                        "role_event_id",
                        "participant_type",
                        "participant_id"
                }
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleEventParticipant extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_event_id", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private RoleEvent roleEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 20, updatable = false)
    private RoleEventParticipantType participantType;

    @Column(name = "participant_id", nullable = false, updatable = false)
    private Long participantId;

    RoleEventParticipant(
            RoleEvent roleEvent,
            RoleEventParticipantType participantType,
            Long participantId
    ) {
        this.roleEvent = Guard.notNull(roleEvent, "roleEvent");
        this.participantType = Guard.notNull(
                participantType,
                "participantType"
        );
        this.participantId = positive(participantId, "participantId");
    }

    boolean matches(
            RoleEventParticipantType type,
            Long candidateId
    ) {
        return participantType == type && participantId.equals(candidateId);
    }

    private static Long positive(Long value, String name) {
        return Guard.minValue(Guard.notNull(value, name), 1L, name);
    }
}
