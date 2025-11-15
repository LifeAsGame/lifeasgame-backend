package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_message_channel_seq", columnList = "channel_id, id"),
                @Index(name = "idx_message_channel_time", columnList = "channel_id, created_at, id"),
                @Index(name = "idx_message_sender", columnList = "sender_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private ChatChannel channel;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    @Version
    private Long version;

    private ChatMessage(ChatChannel channel, Long senderId, String content) {
        Guard.notNull(channel, "channel");
        Guard.notNull(senderId, "senderId");
        Guard.notBlank(content, "content");
        this.channel = channel;
        this.senderId = senderId;
        this.content = content;
    }

    public static ChatMessage create(ChatChannel channel, Long senderId, String content) {
        return new ChatMessage(channel, senderId, content);
    }

    public void edit(String newContent) {
        Guard.notBlank(newContent, "newContent");
        this.content = newContent;
        this.edited = true;
    }
}
