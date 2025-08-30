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
import online.lifeasgame.core.annotation.AggregateRoot;

@Entity
@AggregateRoot
@Table(
        name = "player_hobbies",
        indexes = @Index(name = "idx_hobby_player", columnList = "player_id")
)
public class PlayerHobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(length = 60, nullable = false)
    private String name;

    @Column(length = 200)
    private String detail;
}
