package online.lifeasgame.role.application.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PersonRoleContextQuery {
    Page<Context> findOwned(Long playerId, Long personId, boolean includeArchived, String keyword, Pageable page);

    record Context(Long relationId, Long roleId, Long personId, String roleName, String roleStatus,
                   String relationType, String roleNotes, String relationStatus, Long version) {}
}
