package online.lifeasgame.role.application.query;

import online.lifeasgame.role.application.result.RoleResult;

import java.util.List;
import java.util.Optional;

public interface RoleQuery {
    List<RoleResult.Detail> findActive(Long playerId);

    Optional<RoleResult.Detail> findOwned(Long roleId, Long playerId);
}
