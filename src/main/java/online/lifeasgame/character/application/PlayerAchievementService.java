package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PlayerAchievementService {

    private final PlayerAchievementReader playerAchievementReader;

    public List<PlayerAchievementResult.PlayerAchievementInfo> getPlayerAchievementInfos(Long playerId) {
        List<PlayerAchievementView> playerAchievementViews = playerAchievementReader.getPlayerAchievementInfos(playerId);
        return playerAchievementViews.stream()
                .map(PlayerAchievementResult.PlayerAchievementInfo::from)
                .toList();
    }
}
