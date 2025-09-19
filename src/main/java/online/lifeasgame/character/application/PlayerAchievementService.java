package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerAchievementService {

    private final PlayerAchievementReader playerAchievementReader;

    public List<PlayerAchievementResult.PlayerAchievementInfo> getPlayerAchievementInfos(Long playerId) {
        List<PlayerAchievementView> playerAchievementViews = playerAchievementReader.getPlayerAchievementInfos(playerId);
        return playerAchievementViews.stream()
                .map(PlayerAchievementResult.PlayerAchievementInfo::from)
                .toList();
    }
}
