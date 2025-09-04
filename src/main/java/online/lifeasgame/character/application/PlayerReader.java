package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerReader {

    private final PlayerRepository playerRepository;

    public Player getPlayerInfo(Long playerId) {
        return playerRepository.findById(playerId).orElseThrow(
                () -> new DomainException(PlayerError.PLAYER_NOT_FOUND)
        );
    }
}
