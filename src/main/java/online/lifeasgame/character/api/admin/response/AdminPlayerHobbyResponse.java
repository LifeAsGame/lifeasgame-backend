package online.lifeasgame.character.api.admin.response;

import java.time.LocalDate;

public final class AdminPlayerHobbyResponse {

    private AdminPlayerHobbyResponse() {
    }

    public record Granted(
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
        public static Granted of(
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
            return new Granted(
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
