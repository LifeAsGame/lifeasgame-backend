package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.result.GuildResult;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildEvent;
import online.lifeasgame.social.domain.GuildEventRsvp;
import online.lifeasgame.social.domain.GuildStatus;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.GuildEventRepository;
import online.lifeasgame.social.domain.repository.GuildRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuildEventService {
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final GuildRepository guilds;
    private final GuildEventRepository events;
    private final Clock clock;

    public record Details(String title, String sharedDescription, Instant startsAt, Instant endsAt, String location) {}
    public record Event(Long id, Long guildId, String title, String sharedDescription, Instant startsAt,
                        Instant endsAt, String location, String status, Long createdByPlayerId,
                        int participantCount, boolean myRsvp, Instant createdAt, Instant updatedAt) {}
    public record Participant(Long playerId, Instant joinedAt) {}

    @Transactional(readOnly = true)
    public GuildResult.Page<Event> list(Long guildId, int page, int size) {
        PageRequest paging = page(page, size);
        Long actor = actor();
        requireMember(guildId, actor);
        Page<GuildEvent> rows = events.findByGuildIdOrderByIdDesc(guildId, paging);
        Map<Long, GuildEventRepository.EventTotals> totals = totals(rows.getContent(), actor);
        return GuildResult.Page.of(rows.stream().map(e -> result(e, totals.get(e.getId()))).toList(),
                page, size, rows.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Event detail(Long guildId, Long eventId) {
        Long actor = actor();
        requireMember(guildId, actor);
        return result(event(guildId, eventId, false), actor);
    }

    @Transactional(readOnly = true)
    public GuildResult.Page<Participant> participants(Long guildId, Long eventId, int page, int size) {
        PageRequest paging = page(page, size);
        requireMember(guildId, actor());
        event(guildId, eventId, false);
        Page<GuildEventRsvp> rows = events.findCurrentParticipants(guildId, eventId, paging);
        return GuildResult.Page.of(rows.stream().map(r -> new Participant(r.getPlayerId(), r.getJoinedAt())).toList(),
                page, size, rows.getTotalElements());
    }

    @Transactional
    public Event create(Long guildId, Details details) {
        Guild guild = guild(guildId, true);
        leader(guild, actor());
        GuildEvent saved = events.saveAndFlush(GuildEvent.create(guildId, actor(), details.title(),
                details.sharedDescription(), details.startsAt(), details.endsAt(), details.location()));
        return result(saved, actor());
    }

    @Transactional
    public Event update(Long guildId, Long eventId, Details details) {
        Guild guild = guild(guildId, true);
        leader(guild, actor());
        GuildEvent event = event(guildId, eventId, true);
        event.update(details.title(), details.sharedDescription(), details.startsAt(), details.endsAt(), details.location());
        return result(events.saveAndFlush(event), actor());
    }

    @Transactional
    public Event complete(Long guildId, Long eventId) { return finish(guildId, eventId, true); }

    @Transactional
    public Event cancel(Long guildId, Long eventId) { return finish(guildId, eventId, false); }

    private Event finish(Long guildId, Long eventId, boolean complete) {
        Guild guild = guild(guildId, true);
        leader(guild, actor());
        GuildEvent event = event(guildId, eventId, true);
        if (complete) event.complete(); else event.cancel();
        return result(events.saveAndFlush(event), actor());
    }

    @Transactional
    public Event rsvp(Long guildId, Long eventId) {
        Guild guild = guild(guildId, true);
        member(guild, actor());
        GuildEvent event = event(guildId, eventId, true);
        event.join(actor(), clock.instant());
        return result(events.saveAndFlush(event), actor());
    }

    @Transactional
    public void withdraw(Long guildId, Long eventId) {
        Guild guild = guild(guildId, true);
        member(guild, actor());
        GuildEvent event = event(guildId, eventId, true);
        event.leave(actor());
        events.saveAndFlush(event);
    }

    private Event result(GuildEvent event, Long actor) {
        return result(event, totals(List.of(event), actor).get(event.getId()));
    }

    private Event result(GuildEvent event, GuildEventRepository.EventTotals totals) {
        int count = totals == null ? 0 : Math.toIntExact(totals.getParticipantCount());
        return new Event(event.getId(), event.getGuildId(), event.getTitle(), event.getSharedDescription(),
                event.getStartsAt(), event.getEndsAt(), event.getLocation(), event.getStatus().name(),
                event.getCreatedByPlayerId(), count, totals != null && totals.getMyRsvp() > 0,
                event.getCreatedAt(), event.getUpdatedAt());
    }

    private Map<Long, GuildEventRepository.EventTotals> totals(List<GuildEvent> rows, Long actor) {
        if (rows.isEmpty()) return Map.of();
        return events.totals(rows.stream().map(GuildEvent::getId).toList(), actor).stream()
                .collect(Collectors.toMap(GuildEventRepository.EventTotals::getEventId, Function.identity()));
    }

    private void requireMember(Long guildId, Long actor) {
        if (!guilds.isActiveMember(guildId, actor)) throw error(SocialError.GUILD_NOT_FOUND);
    }

    private Guild guild(Long id, boolean lock) {
        return (lock ? guilds.findForUpdate(id) : guilds.findById(id))
                .orElseThrow(() -> error(SocialError.GUILD_NOT_FOUND));
    }

    private GuildEvent event(Long guildId, Long id, boolean lock) {
        return (lock ? events.findForUpdate(id, guildId) : events.findByIdAndGuildId(id, guildId))
                .orElseThrow(() -> error(SocialError.GUILD_EVENT_NOT_FOUND));
    }

    private static void member(Guild guild, Long actor) {
        if (guild.getStatus() != GuildStatus.ACTIVE || guild.findMember(actor).isEmpty())
            throw error(SocialError.GUILD_NOT_FOUND);
    }

    private static void leader(Guild guild, Long actor) {
        member(guild, actor);
        if (!guild.getLeaderPlayerId().equals(actor)) throw error(SocialError.LEADER_ONLY);
    }

    private static PageRequest page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw error(SocialError.GUILD_EVENT_INVALID_INPUT);
        return PageRequest.of(page, size);
    }

    private Long actor() { return currentPlayerAccessor.currentPlayerIdOrThrow(); }
    private static DomainException error(SocialError code) { return new DomainException(code); }
}
