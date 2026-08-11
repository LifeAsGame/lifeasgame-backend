package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.application.internal.RoleEventLookupApi;
import online.lifeasgame.role.domain.RoleEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class RoleEventLookupService implements RoleEventLookupApi {

    private final RoleEventReader reader;

    @Override
    public RoleEventReference getOwned(Long roleEventId, Long playerId) {
        RoleEvent event = reader.getOwned(roleEventId, playerId);
        return new RoleEventReference(
                event.getId(),
                event.getRoleId(),
                event.getStatus().name()
        );
    }
}
