package online.lifeasgame.character.domain.repository;

import java.util.Optional;
import online.lifeasgame.character.domain.PlayerHobby;

public interface PlayerHobbyRepository {
    PlayerHobby save(PlayerHobby playerHobby);

    Optional<PlayerHobby> findByPlayerIdAndHobbyId(Long playerId, Long hobbyId);

    void deleteByPlayerIdAndHobbyId(Long playerId, Long hobbyId);

    boolean existsByPlayerIdAndHobbyId(Long playerId, Long hobbyId);
}
