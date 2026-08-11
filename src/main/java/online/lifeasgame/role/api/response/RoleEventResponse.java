package online.lifeasgame.role.api.response;

import java.time.Instant;
import java.util.List;

public final class RoleEventResponse {

    private RoleEventResponse() {
    }

    public record Detail(
            Long id,
            Long roleId,
            String title,
            String description,
            Instant startsAt,
            Instant endsAt,
            String status,
            Instant completedAt,
            List<Participant> participants,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
    }

    public record Participant(
            Long participantLinkId,
            String participantType,
            Long participantId
    ) {
    }
}
