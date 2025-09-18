package online.lifeasgame.character.application.command;

import java.time.LocalDate;

public class AdminPlayerHobbyCommand {

    private AdminPlayerHobbyCommand() {
    }

    public record GrantHobby(
            Long playerId,
            Long hobbyId,
            String customName,        // PlayerHobby.customName
            String detail,
            Integer proficiency,
            String status,      // raw enum
            LocalDate startedOn,
            Long xp
    ) {
        public static GrantHobby of(
                Long playerId,
                Long hobbyId,
                String customName,        // PlayerHobby.customName
                String detail,
                Integer proficiency,
                String status,      // raw enum
                LocalDate startedOn,
                Long xp
        ) {
            return new GrantHobby(
                    playerId,
                    hobbyId,
                    customName,
                    detail,
                    proficiency,
                    status,
                    startedOn,
                    xp
            );
        }
    }
}
