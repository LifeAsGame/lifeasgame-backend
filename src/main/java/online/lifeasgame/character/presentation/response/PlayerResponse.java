package online.lifeasgame.character.presentation.response;

public class PlayerResponse {

    private PlayerResponse() {
    }

    public record Created(Long id) {
        public static Created of(Long id) {
            return new Created(id);
        }
    }
}
