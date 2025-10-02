package online.lifeasgame.character.api.admin.response;

public class AdminHobbyResponse {

    private AdminHobbyResponse() {
    }

    public record Info(
            Long hobbyId,
            String name,
            String category
    ) {
        public static Info of(Long hobbyId, String name, String category) {
            return new Info(hobbyId, name, category);
        }
    }
}
