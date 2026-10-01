package online.lifeasgame.social.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.social.domain.RoleParty;
import online.lifeasgame.social.domain.RolePartyInvitation;
import online.lifeasgame.social.domain.repository.RolePartyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RolePartyJpaRepository extends JpaRepository<RoleParty, Long>, RolePartyRepository {
    Page<RoleParty> findByCreatorPlayerIdAndRoleIdOrderByIdDesc(Long creatorPlayerId, Long roleId, Pageable pageable);

    @Query("SELECT p FROM RoleParty p JOIN p.members m WHERE m.playerId = :playerId ORDER BY p.id DESC")
    Page<RoleParty> findMine(@Param("playerId") Long playerId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM RoleParty p WHERE p.id = :id")
    Optional<RoleParty> findForUpdate(@Param("id") Long id);

    @Query("SELECT i FROM RolePartyInvitation i JOIN FETCH i.party p WHERE i.inviteePlayerId = :playerId AND i.status = :status AND i.expiresAt > :now ORDER BY i.id DESC")
    Page<RolePartyInvitation> findPendingInvitations(@Param("playerId") Long playerId, @Param("now") Instant now,
                                                     @Param("status") RolePartyInvitation.Status status, Pageable pageable);
}
