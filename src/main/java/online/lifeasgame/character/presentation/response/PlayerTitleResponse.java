package online.lifeasgame.character.presentation.response;

import java.time.Instant;
import java.util.List;

public class PlayerTitleResponse {

    private PlayerTitleResponse() {
    }

    public record PlayerTitleInfos(List<PlayerTitleInfo> playerTitleInfos) {
        public static PlayerTitleInfos of(List<PlayerTitleInfo> playerTitleInfos) {
            return new PlayerTitleInfos(playerTitleInfos);
        }
    }

    public record PlayerTitleInfo(
            Long titleId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
        public static PlayerTitleInfo of(
                Long titleId,
                String code,
                String name,
                String category,
                String descMd,
                Instant acquiredAt
        ) {
            return new PlayerTitleInfo(titleId, code, name, category, descMd, acquiredAt);
        }
    }
}
