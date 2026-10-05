package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.application.internal.RoleLookupApi;
import online.lifeasgame.social.application.PersonalRoleGroupResult.*;
import online.lifeasgame.social.application.query.PersonalRoleGroupQuery;
import online.lifeasgame.social.domain.PersonalGroupType;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class PersonalRoleGroupFinder {
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final RoleLookupApi roles;
    private final PersonalRoleGroupQuery query;

    public Page<Link> links(Long roleId, PersonalGroupType type, String keyword, int page, int size) {
        Long owner = owner(roleId);
        return query.links(owner, roleId, type, search(keyword), paging(page, size));
    }

    public Page<Group> candidates(Long roleId, PersonalGroupType type, String keyword, int page, int size) {
        Long owner = owner(roleId);
        if (type == null) throw new DomainException(SocialError.PERSONAL_GROUP_INVALID_INPUT);
        return query.candidates(owner, type, search(keyword), paging(page, size));
    }

    private Long owner(Long roleId) {
        Long owner = currentPlayerAccessor.currentPlayerIdOrThrow();
        roles.getOwned(roleId, owner);
        return owner;
    }

    private static String search(String raw) {
        String value = raw == null ? "" : raw.strip();
        if (value.length() > 120) throw new DomainException(SocialError.PERSONAL_GROUP_INVALID_INPUT);
        return value;
    }

    private static PageRequest paging(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new DomainException(SocialError.PERSONAL_GROUP_INVALID_INPUT);
        return PageRequest.of(page, size);
    }
}
