package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.domain.RoleEvent;
import online.lifeasgame.role.domain.repository.RoleEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class RoleEventWriter {

    private final RoleEventRepository repository;

    RoleEvent saveAndFlush(RoleEvent roleEvent) {
        return repository.saveAndFlush(roleEvent);
    }
}
