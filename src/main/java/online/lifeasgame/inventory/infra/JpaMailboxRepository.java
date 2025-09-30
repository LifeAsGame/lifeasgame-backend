package online.lifeasgame.inventory.infra;

import java.util.Optional;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMailboxRepository extends JpaRepository<PlayerMailbox, Long> {
    Optional<PlayerMailbox> findByPlayerId(Long playerId);
}
