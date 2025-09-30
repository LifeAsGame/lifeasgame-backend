package online.lifeasgame.inventory.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import online.lifeasgame.inventory.domain.repository.PlayerMailboxRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerMailboxRepositoryAdapter implements PlayerMailboxRepository {

    private final JpaMailboxRepository jpa;

    @Override
    public Optional<PlayerMailbox> findByPlayerId(Long playerId) {
        return jpa.findByPlayerId(playerId);
    }

    @Override
    public PlayerMailbox save(PlayerMailbox box) {
        return jpa.save(box);
    }
}
