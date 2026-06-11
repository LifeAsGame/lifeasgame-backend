package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerRepositoryAdapter implements PlayerRepository {

    private final JpaPlayerRepository jpa;

    @Override
    public Player save(Player player) {
        return jpa.save(player);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpa.existsByUserId(userId);
    }

    @Override
    public Optional<Player> findById(Long playerId) {
        return jpa.findById(playerId);
    }

    @Override
    public boolean existsById(Long playerId) {
        return jpa.existsById(playerId);
    }

    @Override
    public Optional<Player> findByUserId(Long userId) {
        return jpa.findByUserId(userId);
    }
}
