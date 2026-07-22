package online.lifeasgame.character.domain.growth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@Table(
        name = "player_growth_changes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_player_growth_change_reward_line",
                columnNames = "reward_line_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerGrowthChange extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "reward_line_id", nullable = false)
    private Long rewardLineId;

    @Column(name = "requested_exp", nullable = false)
    private long requestedExp;

    @Column(name = "applied_exp", nullable = false)
    private long appliedExp;

    @Column(name = "leftover_exp", nullable = false)
    private long leftoverExp;

    @Column(name = "before_level", nullable = false)
    private int beforeLevel;

    @Column(name = "after_level", nullable = false)
    private int afterLevel;

    @Column(name = "before_total_exp", nullable = false)
    private long beforeTotalExp;

    @Column(name = "after_total_exp", nullable = false)
    private long afterTotalExp;

    private PlayerGrowthChange(
            Long playerId,
            Long rewardLineId,
            Player.GainResult result
    ) {
        validate(playerId, rewardLineId, result);
        this.playerId = playerId;
        this.rewardLineId = rewardLineId;
        this.requestedExp = result.requestedExp();
        this.appliedExp = result.appliedExp();
        this.leftoverExp = result.leftoverExp();
        this.beforeLevel = result.beforeLevel();
        this.afterLevel = result.afterLevel();
        this.beforeTotalExp = result.beforeTotalExp();
        this.afterTotalExp = result.totalExp();
    }

    public static PlayerGrowthChange rewardExp(
            Long playerId,
            Long rewardLineId,
            Player.GainResult result
    ) {
        return new PlayerGrowthChange(playerId, rewardLineId, result);
    }

    public void assertMatches(Long playerId, Long rewardLineId, long requestedExp) {
        if (!this.playerId.equals(playerId)
                || !this.rewardLineId.equals(rewardLineId)
                || this.requestedExp != requestedExp) {
            throw new DomainException(PlayerError.PLAYER_GROWTH_CHANGE_INCONSISTENT);
        }
    }

    private static void validate(Long playerId, Long rewardLineId, Player.GainResult result) {
        if (playerId == null || playerId <= 0 || result == null) {
            throw new DomainException(PlayerError.PLAYER_GROWTH_CHANGE_INVALID);
        }
        if (rewardLineId == null || rewardLineId <= 0) {
            throw new DomainException(PlayerError.PLAYER_GROWTH_REWARD_LINE_ID_REQUIRED);
        }
        boolean invalidAmounts;
        try {
            invalidAmounts = result.requestedExp() <= 0
                    || result.appliedExp() < 0
                    || result.leftoverExp() < 0
                    || Math.addExact(result.appliedExp(), result.leftoverExp()) != result.requestedExp()
                    || Math.subtractExact(result.totalExp(), result.beforeTotalExp()) != result.appliedExp();
        } catch (ArithmeticException exception) {
            throw new DomainException(PlayerError.PLAYER_GROWTH_CHANGE_INVALID, null, exception);
        }
        if (invalidAmounts
                || result.beforeLevel() < 1
                || result.afterLevel() < result.beforeLevel()
                || result.beforeTotalExp() < 0
                || result.totalExp() < result.beforeTotalExp()) {
            throw new DomainException(PlayerError.PLAYER_GROWTH_CHANGE_INVALID);
        }
    }
}
