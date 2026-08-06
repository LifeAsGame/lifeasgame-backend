package online.lifeasgame.role.domain.repository;

import online.lifeasgame.role.domain.Role;
import java.util.Optional;

public interface RoleRepository {
    Role save(Role role);

    Optional<Role> findByIdAndPlayerId(Long id, Long playerId);
}
