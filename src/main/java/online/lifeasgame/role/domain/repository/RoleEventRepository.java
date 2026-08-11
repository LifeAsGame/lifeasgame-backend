package online.lifeasgame.role.domain.repository;

import online.lifeasgame.role.domain.RoleEvent;

import java.util.List;
import java.util.Optional;

public interface RoleEventRepository {

    RoleEvent saveAndFlush(RoleEvent roleEvent);

    List<RoleEvent> findAllOwned(Long roleId, Long playerId);

    Optional<RoleEvent> findOwned(Long eventId, Long roleId, Long playerId);

    Optional<RoleEvent> findOwnedForUpdate(
            Long eventId,
            Long roleId,
            Long playerId
    );

    Optional<RoleEvent> findByIdAndPlayerId(Long eventId, Long playerId);
}
