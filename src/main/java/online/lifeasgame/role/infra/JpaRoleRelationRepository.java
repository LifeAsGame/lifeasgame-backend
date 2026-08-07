package online.lifeasgame.role.infra;

import online.lifeasgame.role.domain.RoleRelation;
import online.lifeasgame.role.domain.RoleRelationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaRoleRelationRepository extends JpaRepository<RoleRelation, Long> {

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

    List<RoleRelation> findAllByPlayerIdAndRoleIdAndStatusOrderByIdAsc(
            Long playerId,
            Long roleId,
            RoleRelationStatus status
    );
}
