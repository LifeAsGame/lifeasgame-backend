package online.lifeasgame.role.application.result;

import online.lifeasgame.role.domain.RoleEvent;
import online.lifeasgame.role.domain.RoleEventParticipant;

import java.time.Instant;
import java.util.List;

public final class RoleEventResult {

    private RoleEventResult() {
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
        public static Detail from(RoleEvent event) {
            return new Detail(
                    event.getId(),
                    event.getRoleId(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getStartsAt(),
                    event.getEndsAt(),
                    event.getStatus().name(),
                    event.getCompletedAt(),
                    event.getParticipants().stream()
                            .map(Participant::from)
                            .toList(),
                    event.getCreatedAt(),
                    event.getUpdatedAt(),
                    event.getVersion()
            );
        }
    }

    public record Participant(
            Long participantLinkId,
            String participantType,
            Long participantId
    ) {
        public static Participant from(RoleEventParticipant participant) {
            return new Participant(
                    participant.getId(),
                    participant.getParticipantType().name(),
                    participant.getParticipantId()
            );
        }
    }
}
