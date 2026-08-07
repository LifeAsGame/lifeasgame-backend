package online.lifeasgame.role.infra;

import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaRoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByIdAndPlayerId(Long id, Long playerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select role from Role role where role.id = :roleId and role.playerId = :playerId")
    Optional<Role> findByIdAndPlayerIdForUpdate(
            @Param("roleId") Long roleId,
            @Param("playerId") Long playerId
    );

    List<Role> findAllByPlayerIdAndStatusOrderByIdAsc(Long playerId, RoleStatus status);
}
