package online.lifeasgame.lifelog.api.player.response;

import java.util.List;

public final class PlayerLifeLogOverviewResponse {

    private PlayerLifeLogOverviewResponse() {}

    public record Dashboard(
            List<PlayerMediaLogResponse.Info> recentMedia,
            List<PlayerExerciseResponse.Info> recentExercises,
            List<PlayerCollectionResponse.Info> recentCollections
    ) {}
}
