package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository {
    Player save(Player player);

    boolean existsByUserId(Long userId);

    Optional<Player> findById(Long playerId);

    boolean existsById(Long playerId);

    List<Player> findByUserId(Long userId);
}
