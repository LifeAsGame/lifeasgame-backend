package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.person.application.internal.PersonLookupApi.PersonReference;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.role.application.command.RoleRelationCommand;
import online.lifeasgame.role.application.result.RoleRelationResult;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleRelation;
import online.lifeasgame.role.domain.RoleRelationStatus;
import online.lifeasgame.role.domain.RoleRelationType;
import online.lifeasgame.role.domain.RoleStatus;
import online.lifeasgame.role.domain.error.RoleError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleRelationService {

    private final RoleReader roleReader;
    private final RoleRelationReader relationReader;
    private final RoleRelationWriter relationWriter;
    private final PersonLookupApi personLookupApi;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public RoleRelationResult.Detail create(
            Long roleId,
            RoleRelationCommand.Create command
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        RoleRelationType relationType = RoleRelationType.of(command.relationType());
        Role role = roleReader.getOwnedForUpdate(roleId, playerId);
        requireActive(role);

        RoleRelation relation = relationReader.findPair(
                        roleId,
                        command.personId(),
                        playerId
                )
                .orElse(null);
        if (relation != null && relation.getStatus() == RoleRelationStatus.ACTIVE) {
            throw new DomainException(RoleError.ROLE_RELATION_ALREADY_EXISTS);
        }

        PersonReference person = personLookupApi.getOwnedActive(
                command.personId(),
                playerId
        );
        if (relation == null) {
            relation = RoleRelation.create(
                    playerId,
                    roleId,
                    command.personId(),
                    relationType,
                    command.roleNotes()
            );
        } else {
            relation.reactivate(relationType, command.roleNotes());
        }

        try {
            return RoleRelationResult.Detail.from(
                    relationWriter.saveAndFlush(relation),
                    person
            );
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(
                    RoleError.ROLE_RELATION_ALREADY_EXISTS,
                    null,
                    exception
            );
        }
    }

    @Transactional
    public RoleRelationResult.Detail update(
            Long roleId,
            Long relationId,
            RoleRelationCommand.Update command
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        roleReader.getOwned(roleId, playerId);
        RoleRelation relation = relationReader.getOwned(
                relationId,
                roleId,
                playerId
        );
        relation.update(
                RoleRelationType.of(command.relationType()),
                command.roleNotes()
        );
        RoleRelation saved = relationWriter.saveAndFlush(relation);
        return RoleRelationResult.Detail.from(saved, person(saved, playerId));
    }

    @Transactional
    public void archive(Long roleId, Long relationId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        roleReader.getOwned(roleId, playerId);
        RoleRelation relation = relationReader.getOwned(
                relationId,
                roleId,
                playerId
        );
        relation.archive();
        relationWriter.saveAndFlush(relation);
    }

    private PersonReference person(RoleRelation relation, Long playerId) {
        PersonReference person = personLookupApi.findOwnedByIds(
                Set.of(relation.getPersonId()),
                playerId
        ).get(relation.getPersonId());
        if (person == null) {
            throw new DomainException(PersonError.PERSON_NOT_FOUND);
        }
        return person;
    }

    private void requireActive(Role role) {
        if (role.getStatus() == RoleStatus.ARCHIVED) {
            throw new DomainException(RoleError.ROLE_ARCHIVED);
        }
    }
}
