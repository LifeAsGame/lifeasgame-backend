package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(length = 80, nullable = false)
    private String name;

    @Column(length = 80)
    private String issuer;

    private LocalDate acquiredDate;

    private LocalDate expiresDate;
}
