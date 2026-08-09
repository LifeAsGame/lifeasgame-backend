package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerQueryService {

    private final PlayerReader playerReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public PlayerResult.PlayerInfo getPlayerInfo() {
        return getPlayerInfo(currentPlayerAccessor.currentPlayerIdOrThrow());
    }

    public PlayerResult.PlayerInfo getPlayerInfo(Long playerId) {
        Player player = playerReader.getByIdOrThrow(playerId);
        return PlayerResult.PlayerInfo.from(player);
    }

    public PlayerResult.PlayerSummary getPlayerSummary(Long userId) {
        Player player = playerReader.getByUserIdOrThrow(userId);
        return PlayerResult.PlayerSummary.from(player);
    }
}
