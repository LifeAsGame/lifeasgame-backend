package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guild_event_rsvps", uniqueConstraints = @UniqueConstraint(name = "uq_guild_event_rsvp", columnNames = {"guild_event_id", "player_id"}))
public class GuildEventRsvp extends AbstractTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guild_event_id", nullable = false, updatable = false)
    private GuildEvent event;
    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
    @Column(nullable = false)
    private boolean active;

    GuildEventRsvp(GuildEvent event, Long playerId, Instant now) {
        this.event = event;
        this.playerId = playerId;
        this.joinedAt = now;
        this.active = true;
    }

    void activate(Instant now) { if (!active) { active = true; joinedAt = now; } }
    void deactivate() { active = false; }
}
