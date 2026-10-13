package online.lifeasgame.role.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public final class RoleRelationResponse {

    private RoleRelationResponse() {
    }

    @Schema(name = "RoleRelationDetail")
    public record Detail(
            Long id,
            Long personId,
            String personDisplayName,
            Long linkedUserId,
            String relationType,
            String roleNotes,
            String status,
            @Schema(description = "연결된 소유 Person의 실제 상태. 관계 status와 독립적이며 Person 보관은 관계를 보관하지 않음",
                    allowableValues = {"ACTIVE", "ARCHIVED"}, requiredMode = Schema.RequiredMode.REQUIRED)
            String personStatus,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
    }
}
