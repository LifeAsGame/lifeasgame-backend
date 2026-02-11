package online.lifeasgame.lifelog.api.player.response;

import java.util.List;

public final class PlayerLifeLogMetaResponse {

    private PlayerLifeLogMetaResponse() {}

    public record Meta(
            List<String> mediaCategories,
            List<String> watchStatuses,
            List<String> exerciseCategories,
            List<String> collectionCategories
    ) {}
}
