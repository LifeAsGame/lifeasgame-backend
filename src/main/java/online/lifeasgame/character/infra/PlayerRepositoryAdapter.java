package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerRepositoryAdapter implements PlayerRepository {

    private final JpaPlayerRepository jpaRepository;

    @Override
    public Player save(Player player) {
        return jpaRepository.save(player);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    public Optional<Player> findById(Long playerId) {
        return jpaRepository.findById(playerId);
    }

    @Override
    public boolean existsById(Long playerId) {
        return jpaRepository.existsById(playerId);
    }

    @Override
    public List<Player> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId);
    }
}
