package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerExistenceReadApi;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerExistenceReadAdapter implements PlayerExistenceReadApi {

    private final JpaPlayerRepository playerRepository;

    @Override
    public boolean existsByPlayerId(Long playerId) {
        return playerRepository.existsById(playerId);
    }
}
