package online.lifeasgame.person.domain;

import jakarta.persistence.Column;
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
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.LocalDate;

@Entity
@AggregateRoot
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "persons",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_person_id_owner",
                        columnNames = {"id", "owner_player_id"}
                ),
                @UniqueConstraint(
                        name = "uq_person_owner_linked_user",
                        columnNames = {"owner_player_id", "linked_user_id"}
                )
        },
        indexes = @Index(
                name = "idx_person_owner_status",
                columnList = "owner_player_id,status,id"
        )
)
public class Person extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_player_id", nullable = false, updatable = false)
    private Long ownerPlayerId;

    @Column(name = "linked_user_id")
    private Long linkedUserId;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(columnDefinition = "text")
    private String notes;

    private LocalDate birthday;

    @Column(length = 120)
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PersonStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    private Person(
            Long ownerPlayerId,
            String displayName,
            String notes,
            LocalDate birthday,
            String contact
    ) {
        this.ownerPlayerId = positive(ownerPlayerId);
        this.linkedUserId = null;
        this.displayName = requiredDisplayName(displayName);
        this.notes = optional(notes, null, "notes");
        this.birthday = birthday;
        this.contact = optionalContact(contact);
        this.status = PersonStatus.ACTIVE;
    }

    public static Person create(
            Long ownerPlayerId,
            String displayName,
            String notes,
            LocalDate birthday,
            String contact
    ) {
        return new Person(ownerPlayerId, displayName, notes, birthday, contact);
    }

    public void update(
            String displayName,
            String notes,
            LocalDate birthday,
            String contact
    ) {
        if (status == PersonStatus.ARCHIVED) {
            throw new DomainException(PersonError.PERSON_ARCHIVED);
        }
        this.displayName = requiredDisplayName(displayName);
        this.notes = optional(notes, null, "notes");
        this.birthday = birthday;
        this.contact = optionalContact(contact);
    }

    public void archive() {
        status = PersonStatus.ARCHIVED;
    }

    private static Long positive(Long value) {
        return Guard.minValue(
                Guard.notNull(value, "ownerPlayerId"),
                1,
                "ownerPlayerId"
        );
    }

    private static String requiredDisplayName(String value) {
        if (value == null) {
            throw new DomainException(PersonError.INVALID_PERSON_DISPLAY_NAME);
        }
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 80) {
            throw new DomainException(PersonError.INVALID_PERSON_DISPLAY_NAME);
        }
        return normalized;
    }

    private static String optionalContact(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 120) {
            throw new DomainException(PersonError.INVALID_PERSON_CONTACT);
        }
        return normalized;
    }

    private static String optional(
            String value,
            Integer max,
            String name
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        return max == null ? normalized : Guard.maxLength(normalized, max, name);
    }
}
