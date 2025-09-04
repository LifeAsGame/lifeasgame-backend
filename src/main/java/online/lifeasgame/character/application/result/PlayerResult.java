package online.lifeasgame.character.application.result;

public class PlayerResult {

    private PlayerResult() {
    }

    public record Created(Long id) {
        public static Created of(Long id) {
            return new Created(id);
        }
    }
}
