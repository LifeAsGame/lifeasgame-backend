package online.lifeasgame.quest.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestReward {

    @Column(name = "reward_exp", nullable = false)
    private int exp;

    @Convert(converter = RewardStatsConverter.class)
    @AttributeOverride(name = "stats", column = @Column(name = "reward_stats", columnDefinition = "json"))
    private RewardStats stats;

    private QuestReward(int exp, RewardStats stats) {
        Guard.minValue(exp, 0, "rewardExp");
        this.exp = exp;
        this.stats = (stats == null ? RewardStats.empty() : stats);
    }

    public static QuestReward of(int exp, RewardStats stats) {
        return new QuestReward(exp, stats);
    }

    public int exp() {
        return exp;
    }

    public RewardStats stats() {
        return stats;
    }
}
