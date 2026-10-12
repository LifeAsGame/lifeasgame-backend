package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.application.internal.RoleLookupApi;
import online.lifeasgame.social.application.PersonalRoleGroupResult.Identity;
import online.lifeasgame.social.domain.PersonalGroupType;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.PersonalRoleGroupStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonalRoleGroupLinker {
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final RoleLookupApi roles;
    private final PersonalRoleGroupStore store;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Identity link(Long roleId, PersonalGroupType type, Long groupId) {
        Long owner = currentPlayerAccessor.currentPlayerIdOrThrow();
        if (type == null || groupId == null || groupId <= 0)
            throw new DomainException(SocialError.PERSONAL_GROUP_INVALID_INPUT);
        roles.getOwnedActiveForUpdate(roleId, owner);
        if (!store.lockAccessibleGroup(owner, type, groupId)) throw new DomainException(switch (type) {
            case GUILD -> SocialError.GUILD_NOT_FOUND;
            case PARTY -> SocialError.PARTY_NOT_FOUND;
            case ROLE_PARTY -> SocialError.ROLE_PARTY_NOT_FOUND;
        });
        return new Identity(store.add(owner, roleId, type, groupId), roleId, type, groupId);
    }

    @Transactional
    public void unlink(Long roleId, Long linkId) {
        Long owner = currentPlayerAccessor.currentPlayerIdOrThrow();
        roles.getOwned(roleId, owner);
        store.remove(owner, roleId, linkId);
    }
}
