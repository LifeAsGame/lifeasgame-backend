package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.domain.RoleRelation;
import online.lifeasgame.role.domain.repository.RoleRelationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class RoleRelationWriter {

    private final RoleRelationRepository repository;

    RoleRelation saveAndFlush(RoleRelation relation) {
        return repository.saveAndFlush(relation);
    }
}
