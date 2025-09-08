package online.lifeasgame.character.application;

import online.lifeasgame.character.application.result.AdminPlayerResult;
import online.lifeasgame.character.domain.service.LevelingPolicy;
import online.lifeasgame.character.domain.repository.LevelCurveParametersLoader;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.Player.GainResult;
import online.lifeasgame.character.domain.service.PrecomputedLevelingPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminPlayerService {

    private final PlayerReader playerReader;
    private final LevelingPolicy levelingPolicy;

    public AdminPlayerService(PlayerReader playerReader, LevelCurveParametersLoader loader) {
        this.playerReader = playerReader;
        this.levelingPolicy = new PrecomputedLevelingPolicy(loader.load());
    }

    @Transactional
    public AdminPlayerResult.ExpGranted grantExp(Long playerId, long exp) {
        Player player = playerReader.getPlayer(playerId);
        GainResult gainResult = player.gainExp(exp, levelingPolicy);
        return AdminPlayerResult.ExpGranted.of(
                player.getId(),
                gainResult
        );
    }
}
