package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "GuildMember")
@Table(
        name = "guild_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_guild_member_guild_player",
                        columnNames = {"guild_id", "player_id"}
                )
        },
        indexes = {
                @Index(name = "idx_guild_member_guild", columnList = "guild_id"),
                @Index(name = "idx_guild_member_player", columnList = "player_id")
        }
)
public class GuildMember extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guild_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_id", nullable = false)
    private Guild guild;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private GuildMemberRole role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    private GuildMember(
            Guild guild,
            Long playerId,
            GuildMemberRole role,
            LocalDateTime joinedAt
    ) {
        this.guild = guild;
        this.playerId = playerId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public static GuildMember createLeader(Guild guild, Long playerId) {
        return create(guild, playerId, GuildMemberRole.LEADER);
    }

    public static GuildMember createMember(Guild guild, Long playerId) {
        return create(guild, playerId, GuildMemberRole.MEMBER);
    }

    private static GuildMember create(Guild guild, Long playerId, GuildMemberRole role) {
        return new GuildMember(
                guild,
                playerId,
                role,
                LocalDateTime.now()
        );
    }

    public void changeRole(GuildMemberRole role) {
        this.role = role;
    }
}
