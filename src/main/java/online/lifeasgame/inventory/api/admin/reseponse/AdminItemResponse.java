package online.lifeasgame.inventory.api.admin.reseponse;

public final class AdminItemResponse {

    private AdminItemResponse() {}

    public record Created(Long id) {
        public static Created of(Long id) {
            return new Created(id);
        }
    }

    public record Id(Long id) {
        public static Id of(Long id) {
            return new Id(id);
        }
    }

    public record Deleted(Long id) {
        public static Deleted of(Long id) {
            return new Deleted(id);
        }
    }
}

