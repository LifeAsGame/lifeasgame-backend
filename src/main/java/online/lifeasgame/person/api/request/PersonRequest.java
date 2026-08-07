package online.lifeasgame.person.api.request;

import java.time.LocalDate;

public final class PersonRequest {

    private PersonRequest() {
    }

    public record Create(
            String displayName,
            String notes,
            LocalDate birthday,
            String contact
    ) {
    }

    public record Update(
            String displayName,
            String notes,
            LocalDate birthday,
            String contact
    ) {
    }
}
