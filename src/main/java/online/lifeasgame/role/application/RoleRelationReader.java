package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.role.domain.RoleRelation;
import online.lifeasgame.role.domain.error.RoleError;
import online.lifeasgame.role.domain.repository.RoleRelationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class RoleRelationReader {

    private final RoleRelationRepository repository;

    RoleRelation getOwned(Long relationId, Long roleId, Long playerId) {
        return repository.findByIdAndRoleIdAndPlayerId(
                        relationId,
                        roleId,
                        playerId
                )
                .orElseThrow(() -> new DomainException(
                        RoleError.ROLE_RELATION_NOT_FOUND
                ));
    }

    Optional<RoleRelation> findPair(
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
