package online.lifeasgame.character.application.command;

import java.time.LocalDate;

public class PlayerHobbyCommand {

    private PlayerHobbyCommand() {
    }

    public record ChangePlayerHobby(
            Long hobbyId,
            String name,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
        public static ChangePlayerHobby of(
                Long hobbyId,
                String name,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new ChangePlayerHobby(hobbyId, name, detail, proficiency, status, startedOn);
        }
    }

    public record CreatePlayerHobby(
            Long hobbyId,
            String name,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
        public static CreatePlayerHobby of(
                Long hobbyId,
                String name,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new CreatePlayerHobby(hobbyId, name, detail, proficiency, status, startedOn);
        }
    }

    public record GrantHobby(
            Long playerId,
            Long hobbyId,
            String customName,        // PlayerHobby.customName
            String detail,
            Integer proficiency,
            String status,      // raw enum
            LocalDate startedOn
    ) {
        public static GrantHobby of(
                Long playerId,
                Long hobbyId,
                String customName,        // PlayerHobby.customName
                String detail,
                Integer proficiency,
                String status,      // raw enum
                LocalDate startedOn
        ) {
            return new GrantHobby(
                    playerId,
                    hobbyId,
                    customName,
                    detail,
                    proficiency,
                    status,
                    startedOn
            );
        }
    }
}
