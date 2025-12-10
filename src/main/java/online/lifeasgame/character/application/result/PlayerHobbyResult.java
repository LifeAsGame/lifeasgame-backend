package online.lifeasgame.character.application.result;

import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.PlayerHobby;

import java.time.LocalDate;

public final class PlayerHobbyResult {

    private PlayerHobbyResult() {
    }

    public record Info(
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
        public static Info from(PlayerHobbyView v) {
            return new Info(
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

    public record Changed(
            Long hobbyId,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
        public static Changed from(PlayerHobby playerHobby) {
            return new Changed(
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

    public record Created(
            Long playerId,
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
        public static Created from(
                PlayerHobby playerHobby,
                Hobby hobby
        ) {
            return new PlayerHobbyResult.Created(
                    playerHobby.getPlayerId(),
                    playerHobby.getHobbyId(),
                    hobby.getName(),
                    hobby.getCategory().name(),
                    playerHobby.getCustomName(),
                    playerHobby.getDetail(),
                    playerHobby.getProficiency(),
                    playerHobby.getStatus().name(),
                    playerHobby.getStartedOn(),
                    playerHobby.getXp()
            );
        }
    }
}
