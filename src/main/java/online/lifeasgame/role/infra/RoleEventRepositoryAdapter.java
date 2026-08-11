package online.lifeasgame.role.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.domain.RoleEvent;
import online.lifeasgame.role.domain.repository.RoleEventRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleEventRepositoryAdapter implements RoleEventRepository {

    private final JpaRoleEventRepository repository;

    @Override
    public RoleEvent saveAndFlush(RoleEvent roleEvent) {
        return repository.saveAndFlush(roleEvent);
    }

    @Override
    public List<RoleEvent> findAllOwned(Long roleId, Long playerId) {
        return repository.findAllByRoleIdAndPlayerIdOrderByIdAsc(
                roleId,
                playerId
        );
    }

    @Override
    public Optional<RoleEvent> findOwned(
            Long eventId,
            Long roleId,
            Long playerId
    ) {
        return repository.findByIdAndRoleIdAndPlayerId(
                eventId,
                roleId,
                playerId
        );
    }

    @Override
    public Optional<RoleEvent> findOwnedForUpdate(
            Long eventId,
            Long roleId,
            Long playerId
    ) {
        return repository.findOwnedForUpdate(eventId, roleId, playerId);
    }

    @Override
    public Optional<RoleEvent> findByIdAndPlayerId(
            Long eventId,
            Long playerId
    ) {
        return repository.findByIdAndPlayerId(eventId, playerId);
    }
}
