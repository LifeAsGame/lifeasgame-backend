package online.lifeasgame.character.api.admin.response;

import java.time.LocalDate;

public class AdminPlayerHobbyResponse {

    private AdminPlayerHobbyResponse() {
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
        public static AdminPlayerHobbyResponse.GrantedHobby of(
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
            return new AdminPlayerHobbyResponse.GrantedHobby(
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
