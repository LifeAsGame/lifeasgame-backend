package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;

@Entity
@AggregateRoot
@Table(name = "player",
        indexes = @Index(name = "player_idx_user", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player extends AbstractTime {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Embedded
    private Name name;

    @Column(length=20)
    @Enumerated(EnumType.STRING)
    private GenderType gender;

    @Column(length=30)
    private String job;

    @Column(name="guild_id")
    private Long guildId;

    @Embedded
    private Level level = Level.of(1);

    @Embedded
    private Experience exp = Experience.of(0);

    @Embedded
    private Health health = Health.full(100);

    @Embedded
    private Mana mana = Mana.full(50);

    @Embedded
    private CoreStats stats = CoreStats.defaults();

    @Convert(converter = ExtraStatsConverter.class)
    @Column(name = "extra_stats", columnDefinition = "json")
    private ExtraStats extraStats;       // 부가 스탯(사교력/노력 등)

    @Convert(converter = StatusEffectsConverter.class)
    @Column(name = "status_effects", columnDefinition = "json")
    private StatusEffects statusEffects;    // 중독/혼란 등

    @Column(name="title_id")
    private Long titleId;

    @Version
    private Long version;


    private Player(Long id, Long userId, Name name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
    }

    public static Player create(Long userId, String nameRaw){
        return new Player(null, userId, Name.of(nameRaw));
    }
}
