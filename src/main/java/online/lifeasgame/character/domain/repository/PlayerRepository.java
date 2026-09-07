package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.Player;

import java.util.Optional;

public interface PlayerRepository {
    Player save(Player player);

    Player saveAndFlush(Player player);

    Optional<Player> findById(Long playerId);

    Optional<Player> findByIdForUpdate(Long playerId);

    boolean existsById(Long playerId);

    Optional<Player> findByUserId(Long userId);

    Optional<Player> findByUserIdForUpdate(Long userId);
}
