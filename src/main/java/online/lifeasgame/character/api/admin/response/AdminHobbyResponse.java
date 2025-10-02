package online.lifeasgame.character.api.admin.response;

public class AdminHobbyResponse {

    private AdminHobbyResponse() {
    }

    public record HobbyInfo(
            Long hobbyId,
            String name,
            String category
    ) {
        public static HobbyInfo of(Long hobbyId, String name, String category) {
            return new HobbyInfo(hobbyId, name, category);
        }
    }
}
