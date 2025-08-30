package online.lifeasgame.social.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Entity
@AggregateRoot
@Table(
        name = "chat_channels",
        indexes = {
                @Index(name = "idx_channel_type", columnList = "type"),
                @Index(name = "idx_channel_context", columnList = "context_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatChannel extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ChatChannelType type;

    @Embedded
    private ChannelName name;

    @Column(name = "context_id")
    private Long contextId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "low", column = @Column(name = "dm_low_id")),
            @AttributeOverride(name = "high", column = @Column(name = "dm_high_id"))
    })
    private PairKey dmKey;

    @Column(name = "read_only", nullable = false)
    private boolean readOnly = false;

    @Version
    private Long version;

    private ChatChannel(ChatChannelType type, ChannelName name, Long contextId, PairKey dmKey) {
        this.type = type;
        this.name = name;
        this.contextId = contextId;
        this.dmKey = dmKey;
    }

    public static ChatChannel global(String name) {
        return new ChatChannel(ChatChannelType.GLOBAL, ChannelName.of(name), null, null);
    }

    public static ChatChannel guild(Long guildId, String name) {
        return new ChatChannel(ChatChannelType.GUILD, ChannelName.of(name), guildId, null);
    }

    public static ChatChannel party(Long partyId, String name) {
        return new ChatChannel(ChatChannelType.PARTY, ChannelName.of(name), partyId, null);
    }

    public static ChatChannel whisper(Long a, Long b) {
        return new ChatChannel(ChatChannelType.WHISPER, null, null, PairKey.of(a, b));
    }

    public static ChatChannel systemRoom(String name) {
        ChatChannel c = new ChatChannel(ChatChannelType.SYSTEM, ChannelName.of(name), null, null);
        c.readOnly = true;
        return c;
    }
}
