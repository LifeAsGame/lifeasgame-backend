package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_message_channel_seq", columnList = "channel_id, id"),
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

    public ChatMessage(ChatChannel channel, Long senderId, String content) {
        Guard.notBlank(content, "content");
        this.channel = channel;
        this.senderId = senderId;
        this.content = content;
    }

    public void edit(String newContent) {
        Guard.notBlank(newContent, "newContent");
        this.content = newContent;
        this.edited = true;
    }
}
