package online.lifeasgame.character.application.result;

public class AdminHobbyResult {

    private AdminHobbyResult() {
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
