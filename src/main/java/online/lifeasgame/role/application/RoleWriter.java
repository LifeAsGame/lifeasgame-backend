package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.repository.RoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class RoleWriter {

    private final RoleRepository repository;

    Role save(Role role) {
        return repository.save(role);
    }
}
