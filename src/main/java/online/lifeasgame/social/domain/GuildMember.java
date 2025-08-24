package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.entity.AbstractTime;

@Entity
@Table(
        name = "guild_members",
        uniqueConstraints = @UniqueConstraint(name = "pk_guild_member", columnNames = {"guild_id", "player_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuildMember extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_id", nullable = false)
    private Guild guild;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private GuildRole role = GuildRole.MEMBER;

    @Version
    private Long version;

    public GuildMember(Guild guild, Long playerId, GuildRole role) {
        this.guild = guild;
        this.playerId = playerId;
        this.role = role == null ? GuildRole.MEMBER : role;
    }

    public void promoteTo(GuildRole newRole) {
        this.role = newRole;
    }
}
