package online.lifeasgame.character.infra;

import online.lifeasgame.character.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByUserId(Long userId);
}
