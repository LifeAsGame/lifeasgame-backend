package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleStatus;
import online.lifeasgame.role.domain.error.RoleError;
import online.lifeasgame.role.domain.repository.RoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class RoleReader {

    private final RoleRepository repository;

    Role getOwned(Long roleId, Long playerId) {
        return repository.findByIdAndPlayerId(roleId, playerId)
                .orElseThrow(() -> new DomainException(RoleError.ROLE_NOT_FOUND));
    }

    List<Role> findActive(Long playerId) {
        return repository.findAllByPlayerIdAndStatus(playerId, RoleStatus.ACTIVE);
    }
}
