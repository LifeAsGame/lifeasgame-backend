package online.lifeasgame.role.application.query;

import online.lifeasgame.role.application.result.RoleRelationResult;

import java.util.List;
import java.util.Optional;

public interface RoleRelationQuery {

    List<RoleRelationResult.Stored> findActive(Long playerId, Long roleId);

    Optional<RoleRelationResult.Stored> findOwned(
            Long relationId,
            Long roleId,
            Long playerId
    );
}
