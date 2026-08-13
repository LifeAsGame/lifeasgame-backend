package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerAchievementQuery;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import online.lifeasgame.character.domain.error.AchievementError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerAchievementReader {

    private final PlayerAchievementQuery query;

    public List<PlayerAchievementView> getViewsByPlayerId(Long playerId) {
        return query.findViewsByPlayerId(playerId);
    }

    public PlayerAchievementView getViewByPlayerIdAndAchievementIdOrThrow(
            Long playerId,
            Long achievementId
    ) {
        return query.findViewByPlayerIdAndAchievementId(playerId, achievementId)
                .orElseThrow(() -> new DomainException(AchievementError.ACHIEVEMENT_NOT_FOUND));
    }
}
