package online.lifeasgame.social.application.query;

import online.lifeasgame.social.application.PersonalRoleGroupResult.*;
import online.lifeasgame.social.domain.PersonalGroupType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PersonalRoleGroupQuery {
    Page<Link> links(Long owner, Long roleId, PersonalGroupType type, String keyword, Pageable page);
    Page<Group> candidates(Long owner, PersonalGroupType type, String keyword, Pageable page);
}
