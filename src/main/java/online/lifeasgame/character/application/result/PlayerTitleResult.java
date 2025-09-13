package online.lifeasgame.character.application.result;

import java.time.Instant;
import online.lifeasgame.character.application.view.PlayerTitleView;

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
}
