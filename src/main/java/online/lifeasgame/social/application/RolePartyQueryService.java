package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.application.internal.RoleLookupApi;
import online.lifeasgame.social.domain.RoleParty;
import online.lifeasgame.social.domain.RolePartyInvitation;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.RolePartyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RolePartyQueryService {
    private final RolePartyRepository repository;
    private final RoleLookupApi roleLookupApi;
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final Clock clock;

    public RolePartyResult.PageResult<RolePartyResult.Summary> forRole(Long roleId, int page, int size) {
        Long playerId = actor();
        if (!"ACTIVE".equals(roleLookupApi.getOwned(roleId, playerId).status()))
            throw new DomainException(SocialError.ROLE_PARTY_INVALID_STATE);
        Page<RoleParty> rows = repository.findByCreatorPlayerIdAndRoleIdOrderByIdDesc(playerId, roleId, page(page, size));
        return RolePartyResult.PageResult.of(rows, rows.stream().map(RolePartyResult.Summary::from).toList());
    }

    public RolePartyResult.PageResult<RolePartyResult.MyGroup> mine(int page, int size) {
        Long playerId = actor();
        Page<RoleParty> rows = repository.findMine(playerId, page(page, size));
        return RolePartyResult.PageResult.of(rows, rows.stream()
                .map(party -> RolePartyResult.MyGroup.from(party, playerId)).toList());
    }

    public RolePartyResult.PageResult<RolePartyResult.Invitation> myInvitations(int page, int size) {
        Page<RolePartyInvitation> rows = repository.findPendingInvitations(actor(), clock.instant(),
                RolePartyInvitation.Status.PENDING, page(page, size));
        return RolePartyResult.PageResult.of(rows, rows.stream().map(RolePartyResult.Invitation::from).toList());
    }

    public RolePartyResult.Detail detail(Long id) {
        RoleParty party = get(id);
        party.requireMember(actor());
        return RolePartyResult.Detail.from(party);
    }

    public RolePartyResult.PageResult<RolePartyResult.Member> members(Long id, int page, int size) {
        RoleParty party = get(id);
        party.requireMember(actor());
        List<RolePartyResult.Member> all = party.activeMembers().stream()
                .map(RolePartyResult.Member::from).toList();
        page(page, size);
        int start = Math.min((int) Math.min((long) page * size, all.size()), all.size());
        List<RolePartyResult.Member> contents = all.subList(start, Math.min(start + size, all.size()));
        return new RolePartyResult.PageResult<>(contents, page, size, all.size(),
                (all.size() + size - 1) / size);
    }

    private Long actor() { return currentPlayerAccessor.currentPlayerIdOrThrow(); }
    private RoleParty get(Long id) { return repository.findById(id).orElseThrow(() -> new DomainException(SocialError.ROLE_PARTY_NOT_FOUND)); }
    private static PageRequest page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new DomainException(SocialError.ROLE_PARTY_INVALID_INPUT);
        return PageRequest.of(page, size);
    }
}
