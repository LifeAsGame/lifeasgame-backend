package online.lifeasgame.character.api.admin.response;

import java.time.Instant;

public final class AdminPlayerTitleResponse {

    private AdminPlayerTitleResponse() {
    }

    public record Granted(
            Long playerId,
            Long titleId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
        public static Granted of(
                Long playerId,
                Long titleId,
                String code,
                String name,
                String category,
                Instant acquiredAt
        ) {
            return new Granted(
                    playerId,
                    titleId,
                    code,
                    name,
                    category,
                    acquiredAt
            );
        }
    }
}
