package online.lifeasgame.role.api.request;

public final class RoleRequest {

    private RoleRequest() {
    }

    public record Create(
            String roleType,
            String name,
            String description
    ) {
    }

    public record Update(
            String roleType,
            String name,
            String description
    ) {
    }
}
