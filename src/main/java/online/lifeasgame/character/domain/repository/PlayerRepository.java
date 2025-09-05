package online.lifeasgame.character.domain.repository;

import java.util.Optional;
import online.lifeasgame.character.domain.Player;

public interface PlayerRepository {
    Player save(Player player);

    boolean existsByUserId(Long userId);

    Optional<Player> findById(Long playerId);
}
