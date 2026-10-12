package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.social.domain.error.SocialError;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guild_events")
public class GuildEvent extends AbstractTime {
    public enum Status { PLANNED, COMPLETED, CANCELED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "guild_id", nullable = false, updatable = false)
    private Long guildId;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(name = "shared_description", length = 2000)
    private String sharedDescription;
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;
    @Column(length = 200)
    private String location;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status;
    @Column(name = "created_by_player_id", nullable = false, updatable = false)
    private Long createdByPlayerId;
    @Version private long version;
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<GuildEventRsvp> rsvps = new ArrayList<>();

    public static GuildEvent create(Long guildId, Long actor, String title, String description,
                                    Instant startsAt, Instant endsAt, String location) {
        GuildEvent event = new GuildEvent();
        event.guildId = guildId;
        event.createdByPlayerId = actor;
        event.status = Status.PLANNED;
        event.setDetails(title, description, startsAt, endsAt, location);
        return event;
    }

    public void update(String title, String description, Instant startsAt, Instant endsAt, String location) {
        planned();
        setDetails(title, description, startsAt, endsAt, location);
    }

    public void complete() { planned(); status = Status.COMPLETED; }
    public void cancel() { planned(); status = Status.CANCELED; }

    public void join(Long playerId, Instant now) {
        planned();
        GuildEventRsvp old = rsvps.stream().filter(r -> r.getPlayerId().equals(playerId)).findFirst().orElse(null);
        if (old == null) rsvps.add(new GuildEventRsvp(this, playerId, now));
        else old.activate(now);
    }

    public void leave(Long playerId) {
        planned();
        rsvps.stream().filter(r -> r.getPlayerId().equals(playerId)).findFirst().ifPresent(GuildEventRsvp::deactivate);
    }

    public boolean hasRsvp(Long playerId) {
        return rsvps.stream().anyMatch(r -> r.getPlayerId().equals(playerId) && r.isActive());
    }

    private void planned() {
        if (status != Status.PLANNED) throw new DomainException(SocialError.GUILD_EVENT_CONFLICT);
    }

    private void setDetails(String title, String description, Instant start, Instant end, String place) {
        if (title == null || title.isBlank() || title.strip().length() > 120 ||
                description != null && description.length() > 2000 ||
                place != null && (place.isBlank() || place.strip().length() > 200) ||
                start == null || end == null || !end.isAfter(start))
            throw new DomainException(SocialError.GUILD_EVENT_INVALID_INPUT);
        this.title = title.strip();
        this.sharedDescription = description;
        this.startsAt = start;
        this.endsAt = end;
        this.location = place == null ? null : place.strip();
    }
}
