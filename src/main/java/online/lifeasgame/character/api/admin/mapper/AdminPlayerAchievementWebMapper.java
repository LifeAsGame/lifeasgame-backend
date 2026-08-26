package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse;
import online.lifeasgame.character.application.result.PlayerAchievementResult;

import java.util.List;

public final class AdminPlayerAchievementWebMapper {

    private AdminPlayerAchievementWebMapper() {}

    public static AdminPlayerAchievementResponse.Infos toInfos(
            Long playerId,
            List<PlayerAchievementResult.Info> results
    ) {
        return new AdminPlayerAchievementResponse.Infos(
                playerId,
                results.stream()
                        .map(result -> new AdminPlayerAchievementResponse.Info(
                                result.achievementId(),
                                result.code(),
                                result.name(),
                                result.category(),
                                result.acquiredAt()
                        ))
                        .toList()
        );
    }

    public static AdminPlayerAchievementResponse.Granted toGranted(PlayerAchievementResult.Granted result) {
        return new AdminPlayerAchievementResponse.Granted(
                result.playerId(),
                result.achievementId(),
                result.code(),
                result.name(),
                result.category(),
                result.acquiredAt()
        );
    }

    public static AdminPlayerAchievementResponse.Revoked toRevoked(PlayerAchievementResult.Revoked result) {
        return new AdminPlayerAchievementResponse.Revoked(
                result.playerId(),
                result.achievementId()
        );
    }
}
