package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.application.internal.RoleLookupApi;
import online.lifeasgame.social.domain.RoleParty;
import online.lifeasgame.social.domain.RolePartyInvitation;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.RolePartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RolePartyService {
    private final RolePartyRepository repository;
    private final RoleLookupApi roleLookupApi;
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final FriendshipVerifier friendshipVerifier;
    private final Clock clock;

    @Transactional
    public RolePartyResult.Detail create(Long roleId, String name, String description, int maxMembers) {
        Long playerId = actor();
        roleLookupApi.getOwnedActiveForUpdate(roleId, playerId);
        return RolePartyResult.Detail.from(repository.saveAndFlush(RoleParty.create(roleId, playerId, name, description, maxMembers)));
    }

    @Transactional
    public RolePartyResult.Detail update(Long id, String name, String description, int maxMembers) {
        RoleParty party = locked(id);
        party.update(actor(), name, description, maxMembers);
        return RolePartyResult.Detail.from(repository.saveAndFlush(party));
    }

    @Transactional
    public RolePartyResult.Invitation invite(Long id, Long inviteePlayerId) {
        RoleParty party = locked(id);
        Long actor = actor();
        party.requireMember(actor);
        if (!party.getLeaderPlayerId().equals(actor)) throw new DomainException(SocialError.ROLE_PARTY_NOT_FOUND);
        friendshipVerifier.verify(actor, inviteePlayerId);
        party.invite(actor, inviteePlayerId, clock.instant());
        RoleParty saved = repository.saveAndFlush(party);
        return RolePartyResult.Invitation.from(saved.getInvitations().stream()
                .filter(i -> i.getInviteePlayerId().equals(inviteePlayerId)).findFirst().orElseThrow());
    }

    @Transactional
    public RolePartyResult.Detail accept(Long id, Long invitationId) {
        RoleParty party = locked(id);
        party.accept(actor(), invitationId, clock.instant());
        return RolePartyResult.Detail.from(repository.saveAndFlush(party));
    }

    @Transactional
    public void decline(Long id, Long invitationId) {
        RoleParty party = locked(id);
        party.decline(actor(), invitationId, clock.instant());
    }

    @Transactional
    public void cancelInvitation(Long id, Long invitationId) {
        RoleParty party = locked(id);
        party.cancelInvitation(actor(), invitationId);
    }

    @Transactional
    public void leave(Long id) { locked(id).leave(actor()); }

    @Transactional
    public RolePartyResult.Detail transferLeader(Long id, Long toPlayerId) {
        RoleParty party = locked(id);
        party.transferLeader(actor(), toPlayerId);
        return RolePartyResult.Detail.from(repository.saveAndFlush(party));
    }

    @Transactional
    public RolePartyResult.Detail disband(Long id) {
        RoleParty party = locked(id);
        party.disband(actor());
        return RolePartyResult.Detail.from(repository.saveAndFlush(party));
    }

    private Long actor() { return currentPlayerAccessor.currentPlayerIdOrThrow(); }
    private RoleParty locked(Long id) { return repository.findForUpdate(id).orElseThrow(() -> new DomainException(SocialError.ROLE_PARTY_NOT_FOUND)); }
}
