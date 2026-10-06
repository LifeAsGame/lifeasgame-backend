package online.lifeasgame.social.application;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildStatus;
import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyStatus;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.GuildRepository;
import online.lifeasgame.social.domain.repository.PartyRepository;
import online.lifeasgame.social.infra.GroupRosterStore;
import online.lifeasgame.social.infra.GroupRosterStore.Entry;
import online.lifeasgame.social.infra.GroupRosterStore.Invitation;
import online.lifeasgame.social.infra.GroupRosterStore.Type;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupRosterService {
    private final GuildRepository guilds;
    private final PartyRepository parties;
    private final GroupRosterStore store;
    private final CurrentPlayerAccessor currentPlayer;
    private final PlayerLookupApi players;
    private final EntityManager entityManager;
    private final Clock clock;

    public record Capabilities(boolean canManageRoster, boolean canInvite) {}
    public record Page<T>(List<T> contents, int page, int size, long totalElements, int totalPages,
                          Capabilities capabilities) {
        static <T> Page<T> of(List<T> rows, int page, int size, long count, Capabilities capabilities) {
            return new Page<>(rows, page, size, count, (int) ((count + size - 1) / size), capabilities);
        }
    }
    public record InvitationPage(List<Invitation> contents, int page, int size,
                                 long totalElements, int totalPages) {
        static InvitationPage of(List<Invitation> rows, int page, int size, long count) {
            return new InvitationPage(rows, page, size, count, (int) ((count + size - 1) / size));
        }
    }

    @Transactional(readOnly = true)
    public Page<Entry> list(Type type, Long groupId, String status, String keyword, int page, int size) {
        page(page, size);
        if (!List.of("ALL", "UNLINKED", "LINKED").contains(status) || keyword != null && keyword.length() > 80)
            throw error(SocialError.ROSTER_INVALID_INPUT);
        Access access = access(type, groupId, false);
        if (!access.member()) throw hidden(type);
        long count = store.entryCount(type, groupId, status, keyword);
        return Page.of(store.entries(type, groupId, status, keyword, page, size), page, size, count,
                new Capabilities(access.leader(), access.leader()));
    }

    @Transactional
    public Entry create(Type type, Long groupId, String displayName, String groupRoleLabel) {
        Access access = access(type, groupId, true);
        requireLeader(access);
        if (store.unlinkedCount(type, groupId) >= 500) throw error(SocialError.ROSTER_CONFLICT);
        String name = name(displayName);
        String label = label(groupRoleLabel);
        long id = store.create(type, groupId, name, label, clock.instant());
        return store.entry(type, groupId, id, false);
    }

    @Transactional
    public Entry update(Type type, Long groupId, Long id, String displayName, String groupRoleLabel, Long version) {
        requireLeader(access(type, groupId, true));
        Entry entry = entry(type, groupId, id);
        if (version == null || version < 0) throw error(SocialError.ROSTER_INVALID_INPUT);
        if (entry.version() != version || store.update(type, groupId, id, version,
                name(displayName), label(groupRoleLabel), clock.instant()) != 1) throw error(SocialError.ROSTER_CONFLICT);
        return store.entry(type, groupId, id, false);
    }

    @Transactional
    public void delete(Type type, Long groupId, Long id, Long version) {
        requireLeader(access(type, groupId, true));
        Entry entry = entry(type, groupId, id);
        if (version == null || version < 0) throw error(SocialError.ROSTER_INVALID_INPUT);
        if (entry.version() != version || store.delete(type, groupId, id, version, clock.instant()) != 1)
            throw error(SocialError.ROSTER_CONFLICT);
        Invitation pending = store.pendingForEntry(id);
        if (pending != null) store.transition(pending.id(), "PENDING", "CANCELED", clock.instant());
    }

    @Transactional
    public Invitation invite(Type type, Long groupId, Long entryId, Long targetPlayerId) {
        Access access = access(type, groupId, true);
        requireLeader(access);
        Entry entry = entry(type, groupId, entryId);
        if (entry.linkedPlayerId() != null) throw error(SocialError.ROSTER_CONFLICT);
        if (targetPlayerId == null || targetPlayerId <= 0) throw error(SocialError.ROSTER_INVALID_INPUT);
        players.findUserIdByPlayerId(targetPlayerId);
        Instant now = clock.instant();
        Invitation pending = store.pendingForEntry(entryId);
        if (pending != null && !pending.expiresAt().isAfter(now)) {
            store.transition(pending.id(), "PENDING", "EXPIRED", now);
            pending = null;
        }
        if (pending != null) {
            if (!pending.targetPlayerId().equals(targetPlayerId)) throw error(SocialError.ROSTER_CONFLICT);
            return pending;
        }
        long id = store.invite(entryId, targetPlayerId, actor(), now.plusSeconds(7 * 24 * 3600), now);
        return store.invitation(id, false);
    }

    @Transactional(readOnly = true)
    public InvitationPage pending(Type type, Long groupId, int page, int size) {
        page(page, size);
        requireLeader(access(type, groupId, false));
        Instant now = clock.instant();
        long count = store.pendingCount(type, groupId, now);
        return InvitationPage.of(store.pending(type, groupId, now, page, size), page, size, count);
    }

    @Transactional
    public Invitation cancel(Type type, Long groupId, Long invitationId) {
        requireLeader(access(type, groupId, true));
        Invitation invitation = store.invitation(invitationId, false);
        if (invitation == null || invitation.type() != type || !invitation.groupId().equals(groupId))
            throw error(SocialError.ROSTER_INVITATION_NOT_FOUND);
        if (!"PENDING".equals(invitation.status())) throw error(SocialError.ROSTER_CONFLICT);
        store.transition(invitationId, "PENDING", "CANCELED", clock.instant());
        return store.invitation(invitationId, false);
    }

    @Transactional(readOnly = true)
    public InvitationPage mine(int page, int size) {
        page(page, size);
        Instant now = clock.instant();
        long count = store.mineCount(actor(), now);
        return InvitationPage.of(store.mine(actor(), now, page, size), page, size, count);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Invitation decline(Long invitationId) {
        Invitation initial = ownInvitation(invitationId);
        access(initial.type(), initial.groupId(), true);
        Invitation invitation = ownInvitation(invitationId);
        validPending(invitation);
        store.transition(invitationId, "PENDING", "DECLINED", clock.instant());
        return store.invitation(invitationId, false);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Entry accept(Long invitationId) {
        Invitation initial = ownInvitation(invitationId);
        Access access = access(initial.type(), initial.groupId(), true);
        Invitation invitation = ownInvitation(invitationId);
        Entry entry = entry(invitation.type(), invitation.groupId(), invitation.entryId());
        if ("ACCEPTED".equals(invitation.status()) && entry.linkedPlayerId() != null
                && entry.linkedPlayerId().equals(actor()) && access.member()) return entry;
        validPending(invitation);
        if (!access.leaderId().equals(invitation.issuedByPlayerId()) || entry.linkedPlayerId() != null)
            throw error(SocialError.ROSTER_CONFLICT);
        if (!access.member()) {
            if (access.guild() != null) access.guild().joinFromRosterInvitation(actor());
            else access.party().joinFromRosterInvitation(actor());
        }
        try {
            if (store.link(invitation.type(), invitation.groupId(), entry.id(), actor(), clock.instant()) != 1)
                throw error(SocialError.ROSTER_CONFLICT);
        } catch (DuplicateKeyException ex) {
            throw error(SocialError.ROSTER_CONFLICT);
        }
        store.transition(invitationId, "PENDING", "ACCEPTED", clock.instant());
        entityManager.flush();
        return store.entry(invitation.type(), invitation.groupId(), entry.id(), false);
    }

    private Invitation ownInvitation(Long id) {
        Invitation invitation = store.invitation(id, false);
        if (invitation == null || !invitation.targetPlayerId().equals(actor()))
            throw error(SocialError.ROSTER_INVITATION_NOT_FOUND);
        return invitation;
    }

    private void validPending(Invitation invitation) {
        if (!"PENDING".equals(invitation.status()) || !invitation.expiresAt().isAfter(clock.instant()))
            throw error(SocialError.ROSTER_CONFLICT);
        Access access = access(invitation.type(), invitation.groupId(), false);
        if (!access.leaderId().equals(invitation.issuedByPlayerId())) throw error(SocialError.ROSTER_CONFLICT);
    }

    private Entry entry(Type type, Long groupId, Long id) {
        Entry entry = store.entry(type, groupId, id, false);
        if (entry == null || !"ACTIVE".equals(entry.status())) throw error(SocialError.ROSTER_NOT_FOUND);
        return entry;
    }

    private Access access(Type type, Long groupId, boolean lock) {
        Long actor = actor();
        if (type == Type.GUILD) {
            Guild guild = (lock ? guilds.findForUpdate(groupId) : guilds.findById(groupId))
                    .orElseThrow(() -> hidden(type));
            if (guild.getStatus() != GuildStatus.ACTIVE) throw hidden(type);
            return new Access(guild, null, guild.getLeaderPlayerId(), guild.findMember(actor).isPresent(),
                    guild.getLeaderPlayerId().equals(actor) && guild.findMember(actor).isPresent());
        }
        Party party = (lock ? parties.findForUpdate(groupId) : parties.findById(groupId))
                .orElseThrow(() -> hidden(type));
        if (party.getStatus() != PartyStatus.ACTIVE) throw hidden(type);
        return new Access(null, party, party.getLeaderPlayerId(), party.findMember(actor).isPresent(),
                party.getLeaderPlayerId().equals(actor) && party.findMember(actor).isPresent());
    }

    private static String name(String raw) {
        if (raw == null || raw.isBlank() || raw.strip().length() > 80) throw error(SocialError.ROSTER_INVALID_INPUT);
        return raw.strip();
    }

    private static String label(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.strip().length() > 120) throw error(SocialError.ROSTER_INVALID_INPUT);
        return raw.strip();
    }

    private static void page(int page, int size) {
        if (page < 0 || page > 1000 || size < 1 || size > 100) throw error(SocialError.ROSTER_INVALID_INPUT);
    }
    private static void requireLeader(Access access) {
        if (!access.member()) throw hidden(access.guild() == null ? Type.PARTY : Type.GUILD);
        if (!access.leader()) throw error(SocialError.LEADER_ONLY);
    }
    private static DomainException hidden(Type type) {
        return error(type == Type.GUILD ? SocialError.GUILD_NOT_FOUND : SocialError.PARTY_NOT_FOUND);
    }
    private Long actor() { return currentPlayer.currentPlayerIdOrThrow(); }
    private static DomainException error(SocialError code) { return new DomainException(code); }
    private record Access(Guild guild, Party party, Long leaderId, boolean member, boolean leader) {}
}
