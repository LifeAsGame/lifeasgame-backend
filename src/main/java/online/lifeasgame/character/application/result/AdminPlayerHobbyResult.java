package online.lifeasgame.character.application.result;

import java.time.LocalDate;

public class AdminPlayerHobbyResult {

    private AdminPlayerHobbyResult() {
    }

    public record GrantedHobby(
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
        public static AdminPlayerHobbyResult.GrantedHobby of(
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
            return new AdminPlayerHobbyResult.GrantedHobby(
                    playerId,
                    hobbyId,
                    name,
                    category,
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
