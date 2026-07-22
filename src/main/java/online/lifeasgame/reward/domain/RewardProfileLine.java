package online.lifeasgame.reward.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.reward.domain.error.RewardError;

@Getter
@Entity
@Table(
        name = "reward_profile_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reward_profile_line_sort_order",
                columnNames = {"reward_profile_id", "sort_order"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardProfileLine extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_profile_id", nullable = false)
    private RewardProfile profile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_definition_id", nullable = false)
    private RewardDefinition rewardDefinition;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "amount_override")
    private Long amountOverride;

    private RewardProfileLine(
            RewardProfile profile,
            RewardDefinition rewardDefinition,
            int sortOrder,
            Long amountOverride
    ) {
        if (profile == null) {
            throw new DomainException(RewardError.REWARD_PROFILE_LINE_REQUIRED);
        }
        if (rewardDefinition == null) {
            throw new DomainException(RewardError.REWARD_LINE_TARGET_REQUIRED);
        }
        if (sortOrder < 0) {
            throw new DomainException(RewardError.REWARD_LINE_SORT_ORDER_MUST_BE_NON_NEGATIVE);
        }
        if (amountOverride != null && amountOverride <= 0) {
            throw new DomainException(RewardError.REWARD_AMOUNT_OVERRIDE_MUST_BE_POSITIVE);
        }
        this.profile = profile;
        this.rewardDefinition = rewardDefinition;
        this.sortOrder = sortOrder;
        this.amountOverride = amountOverride;
    }

    static RewardProfileLine create(
            RewardProfile profile,
            RewardDefinition rewardDefinition,
            int sortOrder,
            Long amountOverride
    ) {
        return new RewardProfileLine(profile, rewardDefinition, sortOrder, amountOverride);
    }

    public long effectiveAmount() {
        return amountOverride == null ? rewardDefinition.getAmount() : amountOverride;
    }
}
