package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerHolderQueryService {

    private final PlayerAchievementReader playerAchievementReader;
    private final PlayerCertificationReader playerCertificationReader;
    private final PlayerHobbyReader playerHobbyReader;
    private final PlayerTitleReader playerTitleReader;

    public List<PlayerAchievementResult.Info> getAchievementInfos(Long playerId) {
        return playerAchievementReader.getViewsByPlayerId(playerId).stream()
                .map(PlayerAchievementResult.Info::from)
                .toList();
    }

    public List<PlayerCertificationResult.Info> getCertificationInfos(Long playerId) {
        return playerCertificationReader.getViewByPlayerId(playerId).stream()
                .map(PlayerCertificationResult.Info::from)
                .toList();
    }

    public List<PlayerHobbyResult.Info> getHobbyInfos(Long playerId) {
        return playerHobbyReader.getViewsByPlayerId(playerId).stream()
                .map(PlayerHobbyResult.Info::from)
                .toList();
    }

    public List<PlayerTitleResult.Info> getTitleInfos(Long playerId) {
        return playerTitleReader.getViewsByPlayerId(playerId).stream()
                .map(PlayerTitleResult.Info::from)
                .toList();
    }
}
