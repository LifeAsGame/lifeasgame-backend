package online.lifeasgame.role.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.domain.RoleRelation;
import online.lifeasgame.role.domain.repository.RoleRelationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRelationRepositoryAdapter implements RoleRelationRepository {

    private final JpaRoleRelationRepository repository;

    @Override
    public RoleRelation saveAndFlush(RoleRelation relation) {
        return repository.saveAndFlush(relation);
    }

    @Override
    public Optional<RoleRelation> findByIdAndRoleIdAndPlayerId(
            Long id,
            Long roleId,
            Long playerId
    ) {
        return repository.findByIdAndRoleIdAndPlayerId(id, roleId, playerId);
    }

    @Override
    public Optional<RoleRelation> findByRoleIdAndPersonIdAndPlayerId(
            Long roleId,
            Long personId,
            Long playerId
    ) {
        return repository.findByRoleIdAndPersonIdAndPlayerId(
                roleId,
                personId,
                playerId
        );
    }
}
