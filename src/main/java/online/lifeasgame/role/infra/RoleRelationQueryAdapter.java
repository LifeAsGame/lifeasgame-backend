package online.lifeasgame.role.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.application.query.RoleRelationQuery;
import online.lifeasgame.role.application.result.RoleRelationResult;
import online.lifeasgame.role.domain.RoleRelationStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRelationQueryAdapter implements RoleRelationQuery {

    private final JpaRoleRelationRepository repository;

    @Override
    public List<RoleRelationResult.Stored> findActive(
            Long playerId,
            Long roleId
    ) {
        return repository.findAllByPlayerIdAndRoleIdAndStatusOrderByIdAsc(
                        playerId,
                        roleId,
                        RoleRelationStatus.ACTIVE
                ).stream()
                .map(RoleRelationResult.Stored::from)
                .toList();
    }

    @Override
    public Optional<RoleRelationResult.Stored> findOwned(
            Long relationId,
            Long roleId,
            Long playerId
    ) {
        return repository.findByIdAndRoleIdAndPlayerId(
                        relationId,
                        roleId,
                        playerId
                )
                .map(RoleRelationResult.Stored::from);
    }
}
