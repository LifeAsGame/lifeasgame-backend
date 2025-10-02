package online.lifeasgame.character.application.result;

import online.lifeasgame.character.application.view.PlayerTitleView;

import java.time.Instant;

public final class PlayerTitleResult {

    private PlayerTitleResult() {
    }

    public record Info(
            Long titleId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
        public static Info from(PlayerTitleView v) {
            return new Info(
                    v.getTitleId(),
                    v.getCode(),
                    v.getName(),
                    v.getCategory() != null ? v.getCategory().name() : null,
                    v.getDescMd(),
                    v.getAcquiredAt()
            );
        }
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
