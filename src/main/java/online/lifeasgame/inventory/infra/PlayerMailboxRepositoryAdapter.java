package online.lifeasgame.inventory.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.query.MailboxStackQuery;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import online.lifeasgame.inventory.domain.repository.PlayerMailboxRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerMailboxRepositoryAdapter implements PlayerMailboxRepository, MailboxStackQuery {

    private final JpaMailboxRepository jpaRepository;

    @Override
    public Optional<PlayerMailbox> findByPlayerId(Long playerId) {
        return jpaRepository.findByPlayerId(playerId);
    }

    @Override
    public PlayerMailbox save(PlayerMailbox box) {
        return jpaRepository.save(box);
    }

    @Override
    public long countStacksExceeding(Long itemId, int limit) {
        return jpaRepository.countStacksExceeding(itemId, limit);
    }
}
