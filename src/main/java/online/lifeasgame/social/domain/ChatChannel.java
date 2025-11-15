package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.social.domain.error.SocialError;

@Getter
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
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "name", length = 60))
    })
    private ChannelName name;

    @Column(name = "context_id")
    private Long contextId;

    @Column(name = "read_only", nullable = false)
    private boolean readOnly = false;

    @Version
    private Long version;

    private ChatChannel(ChatChannelType type, ChannelName name, Long contextId, boolean readOnly) {
        this.type = Guard.notNull(type, "channelType");
        this.name = name;
        this.contextId = contextId;
        this.readOnly = readOnly;
    }

    public static ChatChannel global(String name) {
        return new ChatChannel(ChatChannelType.GLOBAL, ChannelName.of(name), null, false);
    }

    public static ChatChannel guild(Long guildId, String name) {
        Guard.notNull(guildId, "guildId");
        return new ChatChannel(ChatChannelType.GUILD, ChannelName.of(name), guildId, false);
    }

    public static ChatChannel party(Long partyId, String name) {
        Guard.notNull(partyId, "partyId");
        return new ChatChannel(ChatChannelType.PARTY, ChannelName.of(name), partyId, false);
    }

    public static ChatChannel friend(String name) {
        return new ChatChannel(ChatChannelType.FRIEND, ChannelName.of(name), null, false);
    }

    public static ChatChannel systemRoom(String name) {
        ChatChannel channel = new ChatChannel(ChatChannelType.SYSTEM, ChannelName.of(name), null, true);
        channel.readOnly = true;
        return channel;
    }

    public static ChatChannel admin(Long targetPlayerId, String name) {
        return new ChatChannel(ChatChannelType.ADMIN, ChannelName.of(name), targetPlayerId, false);
    }

    public boolean sameContext(ChatChannelType type, Long contextId) {
        if (this.type != type) {
            return false;
        }
        if (contextId == null) {
            return this.contextId == null;
        }
        return contextId.equals(this.contextId);
    }

    public void rename(String newName) {
        this.name = ChannelName.of(newName);
    }

    public void markReadOnly() {
        this.readOnly = true;
    }

    public void ensureWritable() {
        if (readOnly) {
            throw new DomainException(SocialError.CHAT_CHANNEL_READ_ONLY);
        }
    }
}
