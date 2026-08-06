package online.lifeasgame.person.application.result;

import online.lifeasgame.person.domain.Person;

import java.time.Instant;
import java.time.LocalDate;

public final class PersonResult {

    private PersonResult() {
    }

    public record Detail(
            Long id,
            Long ownerPlayerId,
            Long linkedUserId,
            String displayName,
            String notes,
            LocalDate birthday,
            String contact,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        public static Detail from(Person person) {
            return new Detail(
                    person.getId(),
                    person.getOwnerPlayerId(),
                    person.getLinkedUserId(),
                    person.getDisplayName(),
                    person.getNotes(),
                    person.getBirthday(),
                    person.getContact(),
                    person.getStatus().name(),
                    person.getCreatedAt(),
                    person.getUpdatedAt(),
                    person.getVersion()
            );
        }
    }
}
