package online.lifeasgame.character.application.result;

import online.lifeasgame.character.application.view.PlayerTitleView;

import java.time.Instant;

public class PlayerTitleResult {

    private PlayerTitleResult() {
    }

    public record PlayerTitleInfo(
            Long titleId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
        public static PlayerTitleInfo from(PlayerTitleView v) {
            return new PlayerTitleInfo(
                    v.getTitleId(),
                    v.getCode(),
                    v.getName(),
                    v.getCategory() != null ? v.getCategory().name() : null,
                    v.getDescMd(),
                    v.getAcquiredAt()
            );
        }
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
