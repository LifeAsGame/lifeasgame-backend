package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.application.query.RoleQuery;
import online.lifeasgame.role.application.result.RoleResult;
import online.lifeasgame.role.domain.error.RoleError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleQueryService {

    private final RoleQuery query;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public List<RoleResult.Detail> list() {
        return query.findActive(currentPlayerAccessor.currentPlayerIdOrThrow());
    }

    public RoleResult.Detail detail(Long roleId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return query.findOwned(roleId, playerId)
                .orElseThrow(() -> new DomainException(RoleError.ROLE_NOT_FOUND));
    }
}
