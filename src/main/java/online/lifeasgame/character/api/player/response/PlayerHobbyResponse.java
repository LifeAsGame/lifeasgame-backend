package online.lifeasgame.character.api.player.response;

import java.time.LocalDate;
import java.util.List;

public final class PlayerHobbyResponse {

    private PlayerHobbyResponse() {
    }

    public record Infos(List<Info> infos) {
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
    }

    public record Created(
            Long hobbyId,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
    }
}
