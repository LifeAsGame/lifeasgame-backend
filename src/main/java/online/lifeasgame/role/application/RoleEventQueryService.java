package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.application.result.RoleEventResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleEventQueryService {

    private final RoleReader roleReader;
    private final RoleEventReader eventReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public List<RoleEventResult.Detail> list(Long roleId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        roleReader.getOwned(roleId, playerId);
        return eventReader.findAllOwned(roleId, playerId).stream()
                .map(RoleEventResult.Detail::from)
                .toList();
    }

    public RoleEventResult.Detail detail(Long roleId, Long eventId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return RoleEventResult.Detail.from(eventReader.getOwned(
                eventId,
                roleId,
                playerId
        ));
    }
}
