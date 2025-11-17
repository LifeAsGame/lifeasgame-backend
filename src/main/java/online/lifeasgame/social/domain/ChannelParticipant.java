package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@Table(
        name = "channel_participants",
        uniqueConstraints = @UniqueConstraint(name = "uq_channel_user", columnNames = {"channel_id", "user_id"}),
        indexes = @Index(name = "idx_channel_user", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelParticipant extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private ChatChannel channel;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ChannelRole role = ChannelRole.MEMBER;

    @Version
    private Long version;

    public ChannelParticipant(ChatChannel channel, Long userId, ChannelRole role) {
        this.channel = channel;
        this.userId = userId;
        this.role = role == null ? ChannelRole.MEMBER : role;
    }

    public void changeRole(ChannelRole role) {
        this.role = role;
    }
}
