package online.lifeasgame.character.application.result;

import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.Title;

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

    public record Created(
            Long playerId,
            Long titleId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
        public static Created from(PlayerTitle playerTitle, Title title) {
            return new Created(
                    playerTitle.getPlayerId(),
                    playerTitle.getTitleId(),
                    title.getCode(),
                    title.getName(),
                    title.getCategory().name(),
                    playerTitle.getAcquiredAt()
            );
        }
    }
}
