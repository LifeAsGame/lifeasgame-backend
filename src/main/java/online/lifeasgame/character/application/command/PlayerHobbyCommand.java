package online.lifeasgame.character.application.command;

import java.time.LocalDate;

public final class PlayerHobbyCommand {

    private PlayerHobbyCommand() {
    }

    public record Change(
            Long hobbyId,
            String name,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
    }

    public record Create(
            Long hobbyId,
            String name,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
    }

    public record Grant(
            Long playerId,
            Long hobbyId,
            String customName,        // PlayerHobby.customName
            String detail,
            Integer proficiency,
            String status,      // raw enum
            LocalDate startedOn
    ) {
    }
}
