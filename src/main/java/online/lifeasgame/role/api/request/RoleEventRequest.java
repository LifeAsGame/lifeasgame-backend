package online.lifeasgame.role.api.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import online.lifeasgame.platform.web.validation.CalendarInstantDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class RoleEventRequest {

    private RoleEventRequest() {
    }

    public record Create(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 1000) String description,
            @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant startsAt,
            @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant endsAt
    ) {
    }

    public record Update(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 1000) String description,
            @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant startsAt,
            @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant endsAt
    ) {
    }

    public record AddParticipant(
            @NotBlank String participantType,
            @NotNull @Positive Long participantId
    ) {
    }
}
