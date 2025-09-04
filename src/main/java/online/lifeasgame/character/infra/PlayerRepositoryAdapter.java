package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import org.springframework.stereotype.Repository;

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
}
