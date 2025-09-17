package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.character.domain.error.PlayerCertificationError;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@AggregateRoot
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "player_certifications",
        indexes = @Index(name = "idx_cert_player", columnList = "player_id")
)
public class PlayerCertification extends AbstractTime {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "certification_id", nullable = false)
    private Long certificationId;

    @Column(name = "acquired_date")
    private LocalDate acquiredDate;

    @Column(name = "expires_date")
    private LocalDate expiresDate;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    public PlayerCertification(Long playerId, Long certificationId) {
        this.playerId = playerId;
        this.certificationId = certificationId;
    }

    private PlayerCertification(Long playerId, Long certificationId,
                                LocalDate acquiredDate, LocalDate expiresDate) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.certificationId = Guard.notNull(certificationId, "certificationId");
        this.acquiredDate = acquiredDate;
        this.expiresDate = expiresDate;
        this.grantedAt = Instant.now();
    }

    public static PlayerCertification create(Long playerId, Long certificationId,
                                             LocalDate acquiredDate, LocalDate expiresDate) {
        if (expiresDate != null && expiresDate.isBefore(acquiredDate)) {
            throw new DomainException(PlayerCertificationError.EXPIRES_BEFORE_ACQUIRED);
        }
        return new PlayerCertification(playerId, certificationId, acquiredDate, expiresDate);
    }
}
