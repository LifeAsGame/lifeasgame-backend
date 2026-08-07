package online.lifeasgame.role.api.response;

import java.time.Instant;

public final class RoleRelationResponse {

    private RoleRelationResponse() {
    }

    public record Detail(
            Long id,
            Long personId,
            String personDisplayName,
            Long linkedUserId,
            String relationType,
            String roleNotes,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
    }
}
