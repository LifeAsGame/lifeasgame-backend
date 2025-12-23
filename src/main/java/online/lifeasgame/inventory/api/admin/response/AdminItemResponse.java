package online.lifeasgame.inventory.api.admin.response;

public final class AdminItemResponse {

    private AdminItemResponse() {}

    public record Created(Long id) {
    }

    public record Id(Long id) {
    }

    public record Deleted(Long id) {
    }
}
