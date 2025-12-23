package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "GuildWaitMember")
@Table(
        name = "guild_wait_members", indexes = {
        @Index(name = "idx_wait_guild", columnList = "guild_id"),
        @Index(name = "idx_wait_player", columnList = "player_id")
}
)
public class GuildWaitMember extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guild_wait_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_id", nullable = false)
    private Guild guild;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private GuildWaitType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GuildWaitStatus status;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public GuildWaitMember(
            Guild guild,
            Long playerId,
            GuildWaitType type,
            GuildWaitStatus status,
            String message,
            LocalDateTime requestedAt,
            LocalDateTime expiresAt
    ) {
        this.guild = guild;
        this.playerId = playerId;
        this.type = type;
        this.status = status;
        this.message = message;
        this.requestedAt = requestedAt;
        this.expiresAt = expiresAt;
    }

    public static GuildWaitMember joinRequest(Guild guild, Long playerId, String message) {
        return new GuildWaitMember(
                guild,
                playerId,
                GuildWaitType.JOIN_REQUEST,
                GuildWaitStatus.PENDING,
                message,
                LocalDateTime.now(),
                null
        );
    }

    public static GuildWaitMember invitation(Guild guild, Long invitee, String message, LocalDateTime expiresAt) {
        return new GuildWaitMember(
                guild,
                invitee,
                GuildWaitType.INVITATION,
                GuildWaitStatus.PENDING,
                message,
                LocalDateTime.now(),
                expiresAt
        );
    }

    public void approve() {
        Guard.checkState(this.status == GuildWaitStatus.PENDING, "not pending");
        this.status = GuildWaitStatus.APPROVED;
    }

    public void reject() {
        Guard.checkState(this.status == GuildWaitStatus.PENDING, "not pending");
        this.status = GuildWaitStatus.REJECTED;
    }

    public void cancel() {
        Guard.checkState(this.status == GuildWaitStatus.PENDING, "not pending");
        this.status = GuildWaitStatus.CANCELLED;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
