package online.lifeasgame.character.api.player.response;

import java.time.LocalDate;
import java.util.List;

public final class PlayerHobbyResponse {

    private PlayerHobbyResponse() {
    }

    public record Infos(List<Info> infos) {
        public static Infos of(List<Info> infos) {
            return new Infos(infos);
        }
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
        public static Info of(
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
            return new Info(
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

    public record Changed(
            Long hobbyId,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
        public static Changed of(
                Long hobbyId,
                String customName,
                String detail,
                int proficiency,
                String status,
                LocalDate startedOn,
                long xp
        ) {
            return new Changed(
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

    public record Created(
            Long hobbyId,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
        public static Created of(
                Long hobbyId,
                String customName,
                String detail,
                int proficiency,
                String status,
                LocalDate startedOn,
                long xp
        ) {
            return new Created(
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
