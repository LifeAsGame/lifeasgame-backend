package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.result.GuildResult;
import online.lifeasgame.social.domain.*;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuildGroupLinkService {
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final GuildRepository guilds;
    private final PartyRepository parties;
    private final RolePartyRepository roleParties;
    private final GuildGroupLinkRepository links;

    public record Link(Long id, String groupType, Long groupId, String displayName, String status,
                       String entryAction, Long proposedByPlayerId, boolean guildLeaderApproved,
                       boolean groupLeaderApproved, Instant createdAt, Instant updatedAt) {}
    public record ActiveLink(Long id, String groupType, Long groupId, String displayName,
                             String status, String entryAction) {}
    public record ActivePage(List<ActiveLink> contents, int page, int size, long totalElements,
                             int totalPages, Capabilities capabilities) {}
    public record Capabilities(boolean canStartCreateGroup, boolean rolePartyRequiresOwnedActiveRole) {}

    @Transactional(readOnly = true)
    public ActivePage active(Long guildId, int page, int size) {
        PageRequest paging = page(page, size);
        Guild guild = guild(guildId, false);
        Long actor = actor();
        if (!guilds.isActiveMember(guildId, actor)) throw error(SocialError.GUILD_NOT_FOUND);
        Page<GuildGroupLink> rows = links.findByGuildIdAndStatusOrderByIdDesc(guildId, GuildGroupLink.Status.ACTIVE, paging);
        Map<Long, GuildGroupLinkRepository.LinkTarget> targets = targets(rows.getContent(), actor);
        List<ActiveLink> contents = rows.stream().map(row -> {
            Link link = result(row, targets.get(row.getId()), guild.getLeaderPlayerId());
            return new ActiveLink(link.id(), link.groupType(), link.groupId(), link.displayName(), link.status(), link.entryAction());
        }).toList();
        return new ActivePage(contents, page, size, rows.getTotalElements(), rows.getTotalPages(),
                new Capabilities(true, true));
    }

    @Transactional(readOnly = true)
    public GuildResult.Page<Link> pending(Long guildId, int page, int size) {
        PageRequest paging = page(page, size);
        Guild guild = guild(guildId, false);
        Long actor = actor();
        boolean guildLeader = guild.getLeaderPlayerId().equals(actor) && guilds.isActiveMember(guildId, actor);
        Page<GuildGroupLink> rows = links.findVisiblePending(guildId, actor, guildLeader, paging);
        Map<Long, GuildGroupLinkRepository.LinkTarget> targets = targets(rows.getContent(), actor);
        return GuildResult.Page.of(rows.stream()
                .map(link -> result(link, targets.get(link.getId()), guild.getLeaderPlayerId())).toList(),
                page, size, rows.getTotalElements());
    }

    @Transactional
    public Link propose(Long guildId, String rawType, Long groupId, String displayName) {
        GuildGroupLink.GroupType type = type(rawType);
        if (groupId == null || groupId <= 0) throw error(SocialError.GUILD_GROUP_INVALID_INPUT);
        Guild guild = guild(guildId, true);
        Target target = target(type, groupId, true);
        requireActive(guild, target);
        Long actor = actor();
        boolean guildLeader = leader(guild, actor);
        boolean groupLeader = target.leaderId().equals(actor);
        if (!guildLeader && !groupLeader) throw error(SocialError.GUILD_GROUP_NOT_FOUND);
        GuildGroupLink link = links.findOpen(guildId, type, groupId).orElse(null);
        if (link == null) link = links.saveAndFlush(GuildGroupLink.propose(guildId, type, groupId, displayName, actor, guildLeader, groupLeader));
        else link.requireProposedName(displayName);
        return result(link, target, actor);
    }

    @Transactional
    public Link approve(Long guildId, Long linkId, String displayName) {
        Guild guild = guild(guildId, true);
        GuildGroupLink link = link(guildId, linkId);
        Target target = target(link, true);
        requireActive(guild, target);
        Long actor = actor();
        boolean guildLeader = leader(guild, actor);
        boolean groupLeader = target.leaderId().equals(actor);
        if (!guildLeader && !groupLeader) throw error(SocialError.GUILD_GROUP_NOT_FOUND);
        link.reconcileLeaders(guild.getLeaderPlayerId(), target.leaderId());
        link.approve(actor, guildLeader, groupLeader, displayName);
        return result(links.saveAndFlush(link), target, actor);
    }

    @Transactional
    public Link reject(Long guildId, Long linkId) { return terminate(guildId, linkId, GuildGroupLink.Status.REJECTED); }

    @Transactional
    public Link cancel(Long guildId, Long linkId) { return terminate(guildId, linkId, GuildGroupLink.Status.CANCELED); }

    @Transactional
    public void unlink(Long guildId, Long linkId) { terminate(guildId, linkId, GuildGroupLink.Status.UNLINKED); }

    private Link terminate(Long guildId, Long linkId, GuildGroupLink.Status status) {
        Guild guild = guild(guildId, true);
        GuildGroupLink link = link(guildId, linkId);
        Target target = target(link, true);
        Long actor = actor();
        if (status == GuildGroupLink.Status.CANCELED) {
            if (!link.getProposedByPlayerId().equals(actor)) throw error(SocialError.GUILD_GROUP_NOT_FOUND);
            link.cancel();
        } else {
            if (!leader(guild, actor) && !target.leaderId().equals(actor)) throw error(SocialError.GUILD_GROUP_NOT_FOUND);
            if (status == GuildGroupLink.Status.REJECTED) link.reject();
            else link.unlink();
        }
        return result(links.saveAndFlush(link), target, actor);
    }

    private GuildGroupLink link(Long guildId, Long id) {
        GuildGroupLink link = links.findForUpdate(id).orElseThrow(() -> error(SocialError.GUILD_GROUP_NOT_FOUND));
        if (!link.getGuildId().equals(guildId)) throw error(SocialError.GUILD_GROUP_NOT_FOUND);
        return link;
    }

    private Guild guild(Long id, boolean lock) {
        return (lock ? guilds.findForUpdate(id) : guilds.findById(id))
                .orElseThrow(() -> error(SocialError.GUILD_NOT_FOUND));
    }

    private Target target(GuildGroupLink link, boolean lock) { return target(link.getGroupType(), link.getGroupId(), lock); }

    private Target target(GuildGroupLink.GroupType type, Long id, boolean lock) {
        if (type == GuildGroupLink.GroupType.PARTY) {
            Party party = (lock ? parties.findForUpdate(id) : parties.findById(id))
                    .orElseThrow(() -> error(SocialError.GUILD_GROUP_NOT_FOUND));
            return new Target(party.getLeaderPlayerId(), party.getStatus() == PartyStatus.ACTIVE,
                    party.getVisibility() == PartyVisibility.PUBLIC,
                    player -> party.findMember(player).isPresent());
        }
        RoleParty party = (lock ? roleParties.findForUpdate(id) : roleParties.findById(id))
                .orElseThrow(() -> error(SocialError.GUILD_GROUP_NOT_FOUND));
        return new Target(party.getLeaderPlayerId(), party.getStatus() == RoleParty.Status.ACTIVE,
                false, party::hasMember);
    }

    private Link result(GuildGroupLink link, Target target, Long actor) {
        String action = target.member().test(actor) ? "OPEN_DETAIL" : target.publicPreview() && target.active() ? "OPEN_PUBLIC_PREVIEW" : "INVITE_REQUIRED";
        return new Link(link.getId(), link.getGroupType().name(), link.getGroupId(), link.getDisplayName(),
                link.getStatus().name(), action, link.getProposedByPlayerId(),
                link.getGuildApprovedByPlayerId() != null && link.getGuildApprovedByPlayerId().equals(guild(link.getGuildId(), false).getLeaderPlayerId()),
                link.getGroupApprovedByPlayerId() != null && link.getGroupApprovedByPlayerId().equals(target.leaderId()),
                link.getCreatedAt(), link.getUpdatedAt());
    }

    private Map<Long, GuildGroupLinkRepository.LinkTarget> targets(List<GuildGroupLink> rows, Long actor) {
        if (rows.isEmpty()) return Map.of();
        return links.findTargets(rows.stream().map(GuildGroupLink::getId).toList(), actor).stream()
                .collect(Collectors.toMap(GuildGroupLinkRepository.LinkTarget::getLinkId, Function.identity()));
    }

    private Link result(GuildGroupLink link, GuildGroupLinkRepository.LinkTarget target, Long guildLeader) {
        String action = target.getMember() != 0 ? "OPEN_DETAIL" : target.getPublicPreview() != 0 && target.getActive() != 0
                ? "OPEN_PUBLIC_PREVIEW" : "INVITE_REQUIRED";
        return new Link(link.getId(), link.getGroupType().name(), link.getGroupId(), link.getDisplayName(),
                link.getStatus().name(), action, link.getProposedByPlayerId(),
                guildLeader.equals(link.getGuildApprovedByPlayerId()), target.getLeaderId() != null
                && target.getLeaderId().equals(link.getGroupApprovedByPlayerId()), link.getCreatedAt(), link.getUpdatedAt());
    }

    private void requireActive(Guild guild, Target target) {
        if (guild.getStatus() != GuildStatus.ACTIVE || !target.active()) throw error(SocialError.GUILD_GROUP_CONFLICT);
    }

    private static boolean leader(Guild guild, Long actor) {
        return guild.getStatus() == GuildStatus.ACTIVE && guild.getLeaderPlayerId().equals(actor)
                && guild.findMember(actor).isPresent();
    }

    private static GuildGroupLink.GroupType type(String raw) {
        try { return GuildGroupLink.GroupType.valueOf(raw); }
        catch (RuntimeException ex) { throw error(SocialError.GUILD_GROUP_INVALID_INPUT); }
    }

    private static PageRequest page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw error(SocialError.GUILD_GROUP_INVALID_INPUT);
        return PageRequest.of(page, size);
    }

    private Long actor() { return currentPlayerAccessor.currentPlayerIdOrThrow(); }
    private static DomainException error(SocialError code) { return new DomainException(code); }
    private record Target(Long leaderId, boolean active, boolean publicPreview, java.util.function.Predicate<Long> member) {}
}
