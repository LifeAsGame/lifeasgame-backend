package online.lifeasgame.role.api.response;

import java.time.Instant;

public final class RoleResponse {

    private RoleResponse() {
    }

    public record Detail(
            Long id,
            String roleType,
            String name,
            String description,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
    }
}
