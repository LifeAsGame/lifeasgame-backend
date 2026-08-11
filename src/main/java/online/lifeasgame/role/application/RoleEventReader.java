package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.role.domain.RoleEvent;
import online.lifeasgame.role.domain.error.RoleError;
import online.lifeasgame.role.domain.repository.RoleEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class RoleEventReader {

    private final RoleEventRepository repository;

    List<RoleEvent> findAllOwned(Long roleId, Long playerId) {
        return repository.findAllOwned(roleId, playerId);
    }

    RoleEvent getOwned(Long eventId, Long roleId, Long playerId) {
        return repository.findOwned(eventId, roleId, playerId)
                .orElseThrow(() -> new DomainException(
                        RoleError.ROLE_EVENT_NOT_FOUND
                ));
    }

    RoleEvent getOwned(Long eventId, Long playerId) {
        return repository.findByIdAndPlayerId(eventId, playerId)
                .orElseThrow(() -> new DomainException(
                        RoleError.ROLE_EVENT_NOT_FOUND
                ));
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = false)
    RoleEvent getOwnedForUpdate(
            Long eventId,
            Long roleId,
            Long playerId
    ) {
        return repository.findOwnedForUpdate(eventId, roleId, playerId)
                .orElseThrow(() -> new DomainException(
                        RoleError.ROLE_EVENT_NOT_FOUND
                ));
    }
}
