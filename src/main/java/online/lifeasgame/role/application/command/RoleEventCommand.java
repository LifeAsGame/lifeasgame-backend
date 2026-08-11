package online.lifeasgame.role.application.command;

import java.time.Instant;

public final class RoleEventCommand {

    private RoleEventCommand() {
    }

    public record Create(
            String title,
            String description,
            Instant startsAt,
            Instant endsAt
    ) {
    }

    public record Update(
            String title,
            String description,
            Instant startsAt,
            Instant endsAt
    ) {
    }

    public record AddParticipant(
            String participantType,
            Long participantId
    ) {
    }
}
