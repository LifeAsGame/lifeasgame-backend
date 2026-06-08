package online.lifeasgame.character.infra;

import online.lifeasgame.character.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaPlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByUserId(Long userId);

    Optional<Player> findByUserId(Long userId);
}
