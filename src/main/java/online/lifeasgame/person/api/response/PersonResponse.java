package online.lifeasgame.person.api.response;

import java.time.Instant;
import java.time.LocalDate;

public final class PersonResponse {

    private PersonResponse() {
    }

    public record Detail(
            Long id,
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
    }
}
