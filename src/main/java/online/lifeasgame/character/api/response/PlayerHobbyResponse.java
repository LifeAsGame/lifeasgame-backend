package online.lifeasgame.character.api.response;

import java.time.LocalDate;
import java.util.List;

public class PlayerHobbyResponse {

    private PlayerHobbyResponse() {
    }

    public record PlayerHobbyInfos(List<PlayerHobbyInfo> playerHobbyInfos) {
        public static PlayerHobbyInfos of(List<PlayerHobbyInfo> playerHobbyInfos) {
            return new PlayerHobbyInfos(playerHobbyInfos);
        }
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
        public static PlayerHobbyInfo of(
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
            return new PlayerHobbyInfo(
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

    public record ChangedPlayerHobby(
            Long hobbyId,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
        public static ChangedPlayerHobby of(
                Long hobbyId,
                String customName,
                String detail,
                int proficiency,
                String status,
                LocalDate startedOn,
                long xp
        ) {
            return new ChangedPlayerHobby(
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

    public record CreatedPlayerHobby(
            Long hobbyId,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
        public static CreatedPlayerHobby of(
                Long hobbyId,
                String customName,
                String detail,
                int proficiency,
                String status,
                LocalDate startedOn,
                long xp
        ) {
            return new CreatedPlayerHobby(
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
