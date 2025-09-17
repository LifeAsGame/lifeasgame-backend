package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Entity
@AggregateRoot
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

    private LocalDate acquiredDate;

    private LocalDate expiresDate;
}
