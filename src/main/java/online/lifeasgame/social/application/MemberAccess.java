package online.lifeasgame.social.application;

import online.lifeasgame.social.domain.PersonalGroupType;

public interface MemberAccess {
    void requirePair(PersonalGroupType type, Long groupId, Long actorId, Long memberId, boolean lock);
}
