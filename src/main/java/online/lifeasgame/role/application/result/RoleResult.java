package online.lifeasgame.role.application.result;

import online.lifeasgame.role.domain.Role;

import java.time.Instant;

public final class RoleResult {

    private RoleResult() {
    }

    public record Detail(
            Long id,
            Long playerId,
            String roleType,
            String name,
            String description,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        public static Detail from(Role role) {
            return new Detail(
                    role.getId(),
                    role.getPlayerId(),
                    role.getRoleType().value(),
                    role.getName(),
                    role.getDescription(),
                    role.getStatus().name(),
                    role.getCreatedAt(),
                    role.getUpdatedAt(),
                    role.getVersion()
            );
        }
    }
}
