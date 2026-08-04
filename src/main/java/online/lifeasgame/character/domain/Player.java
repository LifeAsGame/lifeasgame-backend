package online.lifeasgame.character.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.character.domain.converter.ExtraStatsConverter;
import online.lifeasgame.character.domain.converter.GenderTypeConverter;
import online.lifeasgame.character.domain.converter.StatusEffectsEnumConverter;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.event.PlayerLeveledUp;
import online.lifeasgame.character.domain.event.PlayerRegistered;
import online.lifeasgame.character.domain.service.LevelingPolicy;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Convert(converter = StatusEffectsEnumConverter.class)
    private StatusEffects statusEffects;    // 중독/혼란 등

    @Column(name = "title_id")
    private Long titleId;

    @Version
    private Long version;

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

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

    public void markRegistered() {
        if (this.id == null) {
            throw new IllegalStateException("Player must be persisted before marking as registered");
        }
        recordEvent(PlayerRegistered.of(this.id));
    }

    public GainResult gainExp(long amount, LevelingPolicy leveling) {
        if (amount < 1) {
            throw new DomainException(PlayerError.PLAYER_EXP_AMOUNT_MUST_BE_POSITIVE);
        }
        if (leveling == null) {
            throw new DomainException(PlayerError.PLAYER_LEVELING_POLICY_REQUIRED);
        }

        long beforeTotal = this.exp.value();
        int beforeLv = this.level.value();

        long maxBoundary = leveling.totalXpAtLevelStart(leveling.maxLevel()) + leveling.requiredExpFor(leveling.maxLevel());
        long availableCapacity = Math.max(0, maxBoundary - beforeTotal);

        long applied = Math.min(amount, availableCapacity);
        long leftover = amount - applied;

        if (applied > 0) {
            this.exp = this.exp.plus(applied);
        }

        int afterLv = leveling.levelFor(this.exp.value());
        if (afterLv != beforeLv) {
            this.level.with(afterLv);
            if (this.id != null) {
                recordEvent(PlayerLeveledUp.of(this.id, beforeLv, afterLv));
            }
        }

        var p = leveling.progressOf(this.exp.value(), afterLv);

        return new GainResult(
                amount, applied, leftover,
                beforeLv, afterLv,
                beforeTotal,
                this.exp.value(),
                p.expIntoLevel(), p.expToNext(), p.capForLevel(), p.progressRatio()
        );
    }

    public void heal(int amount) {
        this.health = this.health.heal(amount);
    }

    public void damage(int amount) {
        this.health = this.health.damage(amount);
    }

    public void adjustHp(int delta) {
        if (delta >= 0) {
            heal(delta);
        } else {
            try {
                damage(Math.negateExact(delta));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_HP);
            }
        }
    }

    public void increaseMaxHp(int amount) {
        this.health = this.health.increaseCap(amount);
    }

    public void decreaseMaxHp(int amount) {
        this.health = this.health.decreaseCap(amount);
    }

    public void adjustHpCapacity(int delta) {
        if (delta >= 0) {
            increaseMaxHp(delta);
        } else {
            try {
                decreaseMaxHp(Math.negateExact(delta));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_HP_CAPACITY);
            }
        }
    }

    public void restoreMana(int amount) {
        this.mana = this.mana.recover(amount);
    }

    public void spendMana(int amount) {
        this.mana = this.mana.spend(amount);
    }

    public void adjustMana(int delta) {
        if (delta >= 0) {
            restoreMana(delta);
        } else {
            try {
                spendMana(Math.negateExact(delta));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_MP);
            }
        }
    }

    public void increaseMaxMp(int amount) {
        this.mana = this.mana.increaseCap(amount);
    }

    public void decreaseMaxMp(int amount) {
        this.mana = this.mana.decreaseCap(amount);
    }

    public void adjustManaCapacity(int delta) {
        if (delta >= 0) {
            increaseMaxMp(delta);
        } else {
            try {
                decreaseMaxMp(Math.negateExact(delta));
            } catch (ArithmeticException e) {
                throw new DomainException(PlayerError.INVALID_MP_CAPACITY);
            }
        }
    }

    public void grantCoreStats(CoreStatDelta coreStatDelta) {
        this.stats = this.stats.grant(coreStatDelta);
    }

    public void applyStatusEffects(StatusEffects statusEffects) {
        this.statusEffects = this.statusEffects.merged(statusEffects);
    }

    public void changeRepresentativeTitle(Long titleId) {
        this.titleId = titleId;
    }

    public void clearRepresentativeTitleIfMatches(Long revokedTitleId) {
        if (Objects.equals(this.titleId, revokedTitleId)) {
            this.titleId = null;
        }
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(domainEvents);
        domainEvents.clear();
        return copy;
    }

    private void recordEvent(DomainEvent event) {
        if (event == null) return;
        domainEvents.add(event);
    }

    public void rename(Name name) {
        this.name = name;
    }

    public record GainResult(
            long requestedExp,
            long appliedExp,
            long leftoverExp,
            int beforeLevel,
            int afterLevel,
            long beforeTotalExp,
            long totalExp,
            long expIntoLevel,
            long expToNext,
            long capForLevel,
            double progressRatio
    ) {}
}
