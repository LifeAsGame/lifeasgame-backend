package online.lifeasgame.character.application.command;

import java.time.LocalDate;

public class PlayerHobbyCommand {

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
        public static Change of(
                Long hobbyId,
                String name,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new Change(hobbyId, name, detail, proficiency, status, startedOn);
        }
    }

    public record Create(
            Long hobbyId,
            String name,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
        public static Create of(
                Long hobbyId,
                String name,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new Create(hobbyId, name, detail, proficiency, status, startedOn);
        }
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
        public static Grant of(
                Long playerId,
                Long hobbyId,
                String customName,        // PlayerHobby.customName
                String detail,
                Integer proficiency,
                String status,      // raw enum
                LocalDate startedOn
        ) {
            return new Grant(
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
