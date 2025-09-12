package online.lifeasgame.character.application.result;

import java.time.Instant;

public class AdminPlayerTitleResult {

    private AdminPlayerTitleResult() {
    }

    public record GrantedTitle(
            Long playerId,
            Long titleId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
        public static GrantedTitle of(
                Long playerId,
                Long titleId,
                String code,
                String name,
                String category,
                Instant acquiredAt
        ) {
            return new GrantedTitle(
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
