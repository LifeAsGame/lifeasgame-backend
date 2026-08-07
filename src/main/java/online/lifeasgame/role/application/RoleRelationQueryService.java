package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.person.application.internal.PersonLookupApi.PersonReference;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.role.application.query.RoleRelationQuery;
import online.lifeasgame.role.application.result.RoleRelationResult;
import online.lifeasgame.role.domain.error.RoleError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleRelationQueryService {

    private final RoleReader roleReader;
    private final RoleRelationQuery query;
    private final PersonLookupApi personLookupApi;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public List<RoleRelationResult.Detail> list(Long roleId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        roleReader.getOwned(roleId, playerId);
        List<RoleRelationResult.Stored> relations = query.findActive(
                playerId,
                roleId
        );
        Map<Long, PersonReference> persons = persons(relations, playerId);
        return relations.stream()
                .map(relation -> RoleRelationResult.Detail.from(
                        relation,
                        person(relation.personId(), persons)
                ))
                .toList();
    }

    public RoleRelationResult.Detail detail(Long roleId, Long relationId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        roleReader.getOwned(roleId, playerId);
        RoleRelationResult.Stored relation = query.findOwned(
                        relationId,
                        roleId,
                        playerId
                )
                .orElseThrow(() -> new DomainException(
                        RoleError.ROLE_RELATION_NOT_FOUND
                ));
        Map<Long, PersonReference> persons = persons(List.of(relation), playerId);
        return RoleRelationResult.Detail.from(
                relation,
                person(relation.personId(), persons)
        );
    }

    private Map<Long, PersonReference> persons(
            List<RoleRelationResult.Stored> relations,
            Long playerId
    ) {
        Set<Long> personIds = relations.stream()
                .map(RoleRelationResult.Stored::personId)
                .collect(Collectors.toUnmodifiableSet());
        return personLookupApi.findOwnedByIds(personIds, playerId);
    }

    private PersonReference person(
            Long personId,
            Map<Long, PersonReference> persons
    ) {
        PersonReference person = persons.get(personId);
        if (person == null) {
            throw new DomainException(PersonError.PERSON_NOT_FOUND);
        }
        return person;
    }
}
