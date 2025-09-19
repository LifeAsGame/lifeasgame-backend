package online.lifeasgame.character.application.result;

import java.time.LocalDate;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.domain.PlayerHobby;

public class PlayerHobbyResult {

    private PlayerHobbyResult() {
    }

    public record PlayerHobbyInfo(
            Long hobbyId,
            String name,
            String category,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
        public static PlayerHobbyInfo from(PlayerHobbyView v) {
            return new PlayerHobbyInfo(
                    v.getHobbyId(),
                    v.getName(),
                    v.getCategory() != null ? v.getCategory().name() : null,
                    v.getCustomName(),
                    v.getDetail(),
                    v.getProficiency(),
                    v.getStatus() != null ? v.getStatus().name() : null,
                    v.getStartedOn(),
                    v.getXp()
            );
        }
    }

    public record ChangedPlayerHobby(
            Long hobbyId,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
        public static ChangedPlayerHobby from(PlayerHobby playerHobby) {
            return new ChangedPlayerHobby(
                    playerHobby.getHobbyId(),
                    playerHobby.getCustomName(),
                    playerHobby.getDetail(),
                    playerHobby.getProficiency(),
                    playerHobby.getStatus() != null ? playerHobby.getStatus().name() : null,
                    playerHobby.getStartedOn(),
                    playerHobby.getXp()
            );
        }
    }

    public record CreatedPlayerHobby(
            Long hobbyId,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
        public static CreatedPlayerHobby from(PlayerHobby playerHobby) {
            return new CreatedPlayerHobby(
                    playerHobby.getHobbyId(),
                    playerHobby.getCustomName(),
                    playerHobby.getDetail(),
                    playerHobby.getProficiency(),
                    playerHobby.getStatus() != null ? playerHobby.getStatus().name() : null,
                    playerHobby.getStartedOn(),
                    playerHobby.getXp()
            );
        }
    }
}
