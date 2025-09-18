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

@Getter
@Entity
@AggregateRoot
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "player_hobbies",
        indexes = @Index(name = "idx_hobby_player", columnList = "player_id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_player_hobby", columnNames = {"playerId", "hobbyId"})
)
public class PlayerHobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "hobbyId", nullable = false)
    private Long hobbyId;

    @Column(name = "custom_name", length = 60, nullable = false)
    private String customName;

    @Column(name = "detail", length = 200)
    private String detail;

    @Column(name = "proficiency", nullable = false)
    private int proficiency = 0; // 0~100 규약

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
        this.proficiency = proficiency;
        this.status = status;
        this.startedOn = startedOn;
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
            Integer proficiency,
            PlayerHobbyStatus status,
            LocalDate startedOn
    ) {
        this.customName = name;
        this.detail = detail;
        this.proficiency = proficiency;
        this.status = status;
        this.startedOn = startedOn;
    }
}
