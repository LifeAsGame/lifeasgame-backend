package online.lifeasgame.role.api.request;

public final class RoleRelationRequest {

    private RoleRelationRequest() {
    }

    public record Create(
            Long personId,
            String relationType,
            String roleNotes
    ) {
    }

    public record Update(String relationType, String roleNotes) {
    }
}
