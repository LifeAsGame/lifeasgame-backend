package online.lifeasgame.role.domain.repository;

import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleStatus;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Role save(Role role);

    Optional<Role> findByIdAndPlayerId(Long id, Long playerId);

    List<Role> findAllByPlayerIdAndStatus(Long playerId, RoleStatus status);
}
