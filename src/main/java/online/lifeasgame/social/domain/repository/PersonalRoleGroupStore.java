package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.PersonalGroupType;

public interface PersonalRoleGroupStore {
    boolean lockAccessibleGroup(Long owner, PersonalGroupType type, Long groupId);
    Long add(Long owner, Long roleId, PersonalGroupType type, Long groupId);
    void remove(Long owner, Long roleId, Long linkId);
}
