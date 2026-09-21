package online.lifeasgame.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.notification.domain.error.NotificationError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Entity
@AggregateRoot
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "player_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_player_notification_source",
                columnNames = {"player_id", "source_event_id"}
        ),
        indexes = {
                @Index(
                        name = "idx_player_notification_inbox",
                        columnList = "player_id,id"
                ),
                @Index(
                        name = "idx_player_notification_unread",
                        columnList = "player_id,read_at"
                )
        }
)
public class PlayerNotification extends AbstractTime {

    public static final int SOURCE_EVENT_ID_MAX_LENGTH = 255;
    public static final int TITLE_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;

    @Column(nullable = false, updatable = false, length = 40)
    private String type;

    @Column(nullable = false, updatable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String body;

    @Column(
            name = "source_event_id",
            nullable = false,
            updatable = false,
            length = SOURCE_EVENT_ID_MAX_LENGTH
    )
    private String sourceEventId;

    @Column(name = "title_copy_id", updatable = false, length = 100)
    private String titleCopyId;
    @Column(name = "title_copy_version", updatable = false)
    private Integer titleCopyVersion;
    @Column(name = "body_copy_id", updatable = false, length = 100)
    private String bodyCopyId;
    @Column(name = "body_copy_version", updatable = false)
    private Integer bodyCopyVersion;
    @Column(name = "copy_locale", updatable = false, length = 20)
    private String copyLocale;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "read_at")
    private Instant readAt;

    private PlayerNotification(
            Long playerId,
            String sourceEventId,
            NotificationType type,
            String questTitle,
            Instant occurredAt
    ) {
        this.playerId = requiredPlayerId(playerId);
        this.sourceEventId = requiredText(
                sourceEventId,
                SOURCE_EVENT_ID_MAX_LENGTH,
                NotificationError.SOURCE_EVENT_ID_REQUIRED,
                NotificationError.SOURCE_EVENT_ID_TOO_LONG
        );
        NotificationCopy copy = NotificationCopy.forQuest(type, questTitle);
        this.type = type.name();
        this.titleCopyId = copy.titleCopyId();
        this.titleCopyVersion = copy.titleCopyVersion();
        this.bodyCopyId = copy.bodyCopyId();
        this.bodyCopyVersion = copy.bodyCopyVersion();
        this.copyLocale = copy.locale();
        this.title = requiredText(
                copy.title(),
                TITLE_MAX_LENGTH,
                NotificationError.TITLE_REQUIRED,
                NotificationError.TITLE_TOO_LONG
        );
        this.body = requiredBody(copy.body());
        this.occurredAt = required(
                occurredAt,
                NotificationError.OCCURRED_AT_REQUIRED
        );
    }

    public static PlayerNotification create(
            Long playerId,
            String sourceEventId,
            NotificationType type,
            String questTitle,
            Instant occurredAt
    ) {
        return new PlayerNotification(
                playerId,
                sourceEventId,
                type,
                questTitle,
                occurredAt
        );
    }

    public void markRead(Instant time) {
        if (readAt == null) {
            readAt = required(time, NotificationError.READ_AT_REQUIRED);
        }
    }

    private static Long requiredPlayerId(Long playerId) {
        if (playerId == null || playerId <= 0) {
            throw new DomainException(NotificationError.PLAYER_ID_REQUIRED);
        }
        return playerId;
    }

    private static String requiredBody(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(NotificationError.BODY_REQUIRED);
        }
        return value.strip();
    }

    private static String requiredText(
            String value,
            int maxLength,
            NotificationError requiredError,
            NotificationError lengthError
    ) {
        if (value == null || value.isBlank()) {
            throw new DomainException(requiredError);
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new DomainException(lengthError);
        }
        return normalized;
    }

    private static <T> T required(T value, NotificationError error) {
        if (value == null) {
            throw new DomainException(error);
        }
        return value;
    }
}
