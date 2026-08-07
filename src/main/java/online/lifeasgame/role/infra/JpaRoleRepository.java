package online.lifeasgame.role.infra;

import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaRoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByIdAndPlayerId(Long id, Long playerId);

    List<Role> findAllByPlayerIdAndStatusOrderByIdAsc(Long playerId, RoleStatus status);
}
