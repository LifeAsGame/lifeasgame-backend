package online.lifeasgame.role.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@AggregateRoot
@Getter
@Table(
        name = "role_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_role_event_id_role_player",
                columnNames = {"id", "role_id", "player_id"}
        ),
        indexes = @Index(
                name = "idx_role_event_player_role_status",
                columnList = "player_id,role_id,status,id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleEvent extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;

    @Column(name = "role_id", nullable = false, updatable = false)
    private Long roleId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleEventStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(
            mappedBy = "roleEvent",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<RoleEventParticipant> participants = new ArrayList<>();

    private RoleEvent(
            Long playerId,
            Long roleId,
            String title,
            String description,
            Instant startsAt,
            Instant endsAt
    ) {
        this.playerId = positive(playerId, "playerId");
        this.roleId = positive(roleId, "roleId");
        applyStructure(title, description, startsAt, endsAt);
        this.status = RoleEventStatus.PLANNED;
    }

    public static RoleEvent create(
            Long playerId,
            Long roleId,
            String title,
            String description,
            Instant startsAt,
            Instant endsAt
    ) {
        return new RoleEvent(
                playerId,
                roleId,
                title,
                description,
                startsAt,
                endsAt
        );
    }

    public void update(
            String title,
            String description,
            Instant startsAt,
            Instant endsAt
    ) {
        requirePlanned();
        applyStructure(title, description, startsAt, endsAt);
    }

    public void complete(Instant completedAt) {
        requirePlanned();
        this.status = RoleEventStatus.COMPLETED;
        this.completedAt = Guard.notNull(completedAt, "completedAt");
    }

    public void cancel() {
        requirePlanned();
        this.status = RoleEventStatus.CANCELED;
    }

    public RoleEventParticipant addParticipant(
            RoleEventParticipantType type,
            Long participantId
    ) {
        requirePlanned();
        if (participants.stream().anyMatch(
                participant -> participant.matches(type, participantId)
        )) {
            throw new DomainException(
                    RoleError.ROLE_EVENT_PARTICIPANT_ALREADY_EXISTS
            );
        }
        RoleEventParticipant participant = new RoleEventParticipant(
                this,
                type,
                participantId
        );
        participants.add(participant);
        return participant;
    }

    public void removeParticipant(Long participantLinkId) {
        requirePlanned();
        boolean removed = participants.removeIf(
                participant -> participant.getId().equals(participantLinkId)
        );
        if (!removed) {
            throw new DomainException(
                    RoleError.ROLE_EVENT_PARTICIPANT_NOT_FOUND
            );
        }
    }

    public List<RoleEventParticipant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    private void applyStructure(
            String title,
            String description,
            Instant startsAt,
            Instant endsAt
    ) {
        if (startsAt != null && endsAt != null && endsAt.isBefore(startsAt)) {
            throw new DomainException(RoleError.INVALID_ROLE_EVENT_TIME_RANGE);
        }
        this.title = requiredTitle(title);
        this.description = optionalDescription(description);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    private void requirePlanned() {
        if (status != RoleEventStatus.PLANNED) {
            throw new DomainException(RoleError.ROLE_EVENT_NOT_PLANNED);
        }
    }

    private static String requiredTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(RoleError.INVALID_ROLE_EVENT_TITLE);
        }
        String normalized = value.strip();
        if (normalized.length() > 120) {
            throw new DomainException(RoleError.INVALID_ROLE_EVENT_TITLE);
        }
        return normalized;
    }

    private static String optionalDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 1000) {
            throw new DomainException(
                    RoleError.INVALID_ROLE_EVENT_DESCRIPTION
            );
        }
        return normalized;
    }

    private static Long positive(Long value, String name) {
        return Guard.minValue(Guard.notNull(value, name), 1L, name);
    }
}
