package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.character.domain.error.PlayerHobbyError;
import online.lifeasgame.core.error.DomainException;
import java.util.Locale;

@Getter
@Entity
@AggregateRoot
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "player_hobbies",
        indexes = @Index(name = "idx_hobby_player", columnList = "player_id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_player_hobby", columnNames = {"player_id", "hobby_id"})
)
public class PlayerHobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "hobby_id")
    private Long hobbyId;

    @Column(name = "source", nullable = false, length = 16)
    private String source = "CATALOG";

    @Column(name = "private_name_key", length = 60)
    private String privateNameKey;

    @Column(name = "personal_category_id")
    private Long personalCategoryId;

    @Column(name = "custom_name", length = 60, nullable = false)
    private String customName;

    @Column(name = "detail", length = 200)
    private String detail;

    @Column(name = "proficiency", nullable = false)
    private int proficiency = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PlayerHobbyStatus status;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "xp", nullable = false)
    private long xp;

    public PlayerHobby(
            Long playerId,
            Long hobbyId,
            String customName,
            String detail,
            int proficiency,
            PlayerHobbyStatus status,
            LocalDate startedOn
    ) {
        this.playerId = playerId;
        this.hobbyId = hobbyId;
        this.customName = customName;
        this.detail = detail;
        this.proficiency = validateProficiency(proficiency);
        this.status = status;
        this.startedOn = startedOn;
        this.xp = 0;
    }

    public static PlayerHobby create(
            Long playerId,
            Long hobbyId,
            String name,
            String detail,
            int proficiency,
            PlayerHobbyStatus status,
            LocalDate startedOn

    ) {
        return new PlayerHobby(
                playerId,
                hobbyId,
                name,
                detail,
                proficiency,
                status,
                startedOn
        );
    }

    public void changeHobby(
            String name,
            String detail,
            int proficiency,
            PlayerHobbyStatus status,
            LocalDate startedOn
    ) {
        int validatedProficiency = validateProficiency(proficiency);
        this.customName = name;
        this.detail = detail;
        this.proficiency = validatedProficiency;
        this.status = status;
        this.startedOn = startedOn;
    }

    public static PlayerHobby createPrivate(Long playerId, String name, String detail, int proficiency,
                                            PlayerHobbyStatus status, LocalDate startedOn) {
        String normalized = normalizePrivateName(name);
        validatePrivateDetail(detail);
        PlayerHobby hobby = create(playerId, null, name.strip(), detail, proficiency, status, startedOn);
        hobby.source = "PRIVATE";
        hobby.privateNameKey = normalized;
        return hobby;
    }

    public void changePrivate(String name, String detail, int proficiency, PlayerHobbyStatus status, LocalDate startedOn) {
        if (!"PRIVATE".equals(source)) throw new DomainException(PlayerHobbyError.PLAYER_HOBBY_NOT_FOUND);
        String normalized = normalizePrivateName(name);
        validatePrivateDetail(detail);
        changeHobby(name.strip(), detail, proficiency, status, startedOn);
        this.privateNameKey = normalized;
    }

    private static String normalizePrivateName(String name) {
        if (name == null || name.isBlank() || name.strip().codePointCount(0, name.strip().length()) > 60) {
            throw new DomainException(PlayerHobbyError.INVALID_PRIVATE_HOBBY_NAME);
        }
        String normalized = name.strip().toLowerCase(Locale.ROOT);
        if (normalized.codePointCount(0, normalized.length()) > 60) {
            throw new DomainException(PlayerHobbyError.INVALID_PRIVATE_HOBBY_NAME);
        }
        return normalized;
    }

    private static void validatePrivateDetail(String detail) {
        if (detail != null && detail.codePointCount(0, detail.length()) > 200) {
            throw new DomainException(PlayerHobbyError.INVALID_PRIVATE_HOBBY_DETAIL);
        }
    }

    public void assignPersonalCategory(Long personalCategoryId) {
        this.personalCategoryId = personalCategoryId;
    }

    private static int validateProficiency(int proficiency) {
        return Guard.inRange(proficiency, 0, 100, "proficiency");
    }
}
