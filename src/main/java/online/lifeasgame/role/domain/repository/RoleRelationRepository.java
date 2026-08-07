package online.lifeasgame.role.domain.repository;

import online.lifeasgame.role.domain.RoleRelation;

import java.util.Optional;

public interface RoleRelationRepository {

    RoleRelation saveAndFlush(RoleRelation relation);

    Optional<RoleRelation> findByIdAndRoleIdAndPlayerId(
            Long id,
            Long roleId,
            Long playerId
    );

    Optional<RoleRelation> findByRoleIdAndPersonIdAndPlayerId(
            Long roleId,
            Long personId,
            Long playerId
    );
}
