package online.lifeasgame.person.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class PersonRequest {

    private PersonRequest() {
    }

    public record Create(
            @NotBlank @Size(max = 80) String displayName,
            String notes,
            LocalDate birthday,
            @Size(max = 120) String contact
    ) {
    }

    public record Update(
            @NotBlank @Size(max = 80) String displayName,
            String notes,
            LocalDate birthday,
            @Size(max = 120) String contact
    ) {
    }
}
