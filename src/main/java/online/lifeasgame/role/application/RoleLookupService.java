package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.application.internal.RoleLookupApi;
import online.lifeasgame.role.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class RoleLookupService implements RoleLookupApi {

    private final RoleReader reader;

    @Override
    public RoleReference getOwned(Long roleId, Long playerId) {
        Role role = reader.getOwned(roleId, playerId);
        return new RoleReference(
                role.getId(),
                role.getName(),
                role.getStatus().name()
        );
    }
}
