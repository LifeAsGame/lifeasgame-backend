package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.RoleParty;
import online.lifeasgame.social.domain.RolePartyInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface RolePartyRepository {
    RoleParty saveAndFlush(RoleParty party);
    Optional<RoleParty> findById(Long id);
    Optional<RoleParty> findForUpdate(Long id);
    Page<RoleParty> findByCreatorPlayerIdAndRoleIdOrderByIdDesc(Long creatorPlayerId, Long roleId, Pageable pageable);
    Page<RoleParty> findMine(Long playerId, Pageable pageable);
    Page<RolePartyInvitation> findPendingInvitations(Long playerId, Instant now, RolePartyInvitation.Status status, Pageable pageable);
}
