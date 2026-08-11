package online.lifeasgame.role.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.role.domain.RoleEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaRoleEventRepository extends JpaRepository<RoleEvent, Long> {

    @EntityGraph(attributePaths = "participants")
    List<RoleEvent> findAllByRoleIdAndPlayerIdOrderByIdAsc(
            Long roleId,
            Long playerId
    );

    @EntityGraph(attributePaths = "participants")
    Optional<RoleEvent> findByIdAndRoleIdAndPlayerId(
            Long eventId,
            Long roleId,
            Long playerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "participants")
    @Query("""
            SELECT event
            FROM RoleEvent event
            WHERE event.id = :eventId
              AND event.roleId = :roleId
              AND event.playerId = :playerId
            """)
    Optional<RoleEvent> findOwnedForUpdate(
            @Param("eventId") Long eventId,
            @Param("roleId") Long roleId,
            @Param("playerId") Long playerId
    );

    Optional<RoleEvent> findByIdAndPlayerId(Long eventId, Long playerId);
}
