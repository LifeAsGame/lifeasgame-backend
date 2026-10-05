package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.role.application.query.PersonRoleContextQuery;
import online.lifeasgame.role.application.query.PersonRoleContextQuery.Context;
import online.lifeasgame.role.domain.error.RoleError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonRoleContextFinder {
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final PersonLookupApi persons;
    private final PersonRoleContextQuery query;

    public Page<Context> find(Long personId, boolean includeArchived, String keyword, int page, int size) {
        Long owner = currentPlayerAccessor.currentPlayerIdOrThrow();
        if (!persons.findOwnedByIds(Set.of(personId), owner).containsKey(personId))
            throw new DomainException(PersonError.PERSON_NOT_FOUND);
        String search = keyword == null ? "" : keyword.strip();
        if (page < 0 || size < 1 || size > 100 || search.length() > 120)
            throw new DomainException(RoleError.INVALID_ROLE_CONTEXT_QUERY);
        return query.findOwned(owner, personId, includeArchived, search, PageRequest.of(page, size));
    }
}
