package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.character.domain.converter.ExtraStatsConverter;
import online.lifeasgame.character.domain.converter.GenderTypeConverter;
import online.lifeasgame.character.domain.converter.StatusEffectsConverter;
import online.lifeasgame.character.domain.service.LevelingPolicy;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
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
    @Convert(converter = GenderTypeConverter.class)
    private GenderType gender;

    @Column(length=30)
    private String job;

    @Column(name="guild_id")
    private Long guildId;

    @Embedded
    private Level level;

    @Embedded
    private Experience exp;

    @Embedded
    private Health health;

    @Embedded
    private Mana mana;

    @Embedded
    private CoreStats stats;

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

    private Player(Long userId, Name name, GenderType gender) {
        this.userId = Guard.notNull(userId, "userId");
        this.name = Guard.notNull(name, "name");
        this.gender = Guard.notNull(gender, "gender");
        this.level  = Level.of(1);
        this.exp    = Experience.of(0);
        this.health = Health.full(100);
        this.mana   = Mana.full(50);
        this.stats  = CoreStats.defaults();
        this.extraStats   = ExtraStats.empty();
        this.statusEffects = StatusEffects.empty();
    }

    public static Player linkStart(Long userId, Name name, GenderType gender) {
        return new Player(userId, name, gender);
    }

    public GainResult gainExp(long delta, LevelingPolicy leveling) {
        Guard.minValue(delta, 1, "exp delta");
        Guard.notNull(leveling, "leveling");

        long beforeTotal = this.exp.value();
        int beforeLv = this.level.value();

        long maxBoundary = leveling.totalXpAtLevelStart(leveling.maxLevel()) + leveling.requiredExpFor(leveling.maxLevel());
        long availableCapacity = Math.max(0, maxBoundary - beforeTotal);

        long applied = Math.min(delta, availableCapacity);
        long leftover = delta - applied;

        if (applied > 0) {
            this.exp = this.exp.plus(applied);
        }

        int afterLv = leveling.levelFor(this.exp.value());
        if (afterLv != beforeLv) {
            this.level.with(afterLv);
            // addEvent(new PlayerLeveledUp(this.id, afterLv-beforeLv));
        }

        var p = leveling.progressOf(this.exp.value(), afterLv);

        return new GainResult(
                delta, applied, leftover,
                beforeLv, afterLv,
                this.exp.value(),
                p.expIntoLevel(), p.expToNext(), p.capForLevel(), p.progressRatio()
        );
    }

    public void heal(int hp) {
        this.health = this.health.heal(hp);
    }

    public void damage(int hp) {
        this.health = this.health.damage(hp);
    }

    public void increaseMaxHp(int hpCapacity) {
        this.health = this.health.increaseCap(hpCapacity);
    }

    public void decreaseMaxHp(int hpCapacity) {
        this.health = this.health.decreaseCap(hpCapacity);
    }

    public record GainResult(
            long requestedExp,
            long appliedExp,
            long leftoverExp,
            int beforeLevel,
            int afterLevel,
            long totalExp,
            long expIntoLevel,
            long expToNext,
            long capForLevel,
            double progressRatio
    ) {}
}
