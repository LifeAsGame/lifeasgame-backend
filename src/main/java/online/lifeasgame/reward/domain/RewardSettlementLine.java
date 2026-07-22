package online.lifeasgame.reward.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import online.lifeasgame.core.error.ErrorCode;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.reward.domain.error.RewardError;

@Getter
@Entity
@Table(
        name = "reward_settlement_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reward_settlement_line_sort_order",
                columnNames = {"reward_settlement_id", "sort_order"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardSettlementLine extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_settlement_id", nullable = false)
    private RewardSettlement settlement;

    @Column(name = "reward_definition_id", nullable = false)
    private Long rewardDefinitionId;

    @Column(name = "reward_definition_code", length = 80, nullable = false)
    private String rewardDefinitionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", length = 20, nullable = false)
    private RewardType rewardType;

    @Column(nullable = false)
    private long amount;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RewardSettlementLineStatus status;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    private RewardSettlementLine(
            RewardSettlement settlement,
            RewardDefinition definition,
            int sortOrder,
            long amount
    ) {
        if (definition.getId() == null) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_DEFINITION_ID_REQUIRED);
        }
        this.settlement = settlement;
        this.rewardDefinitionId = definition.getId();
        this.rewardDefinitionCode = definition.getCode();
        this.rewardType = definition.getRewardType();
        this.amount = amount;
        this.itemId = definition.getItemId();
        this.sortOrder = sortOrder;
        this.status = RewardSettlementLineStatus.PENDING;
    }

    static RewardSettlementLine snapshot(
            RewardSettlement settlement,
            RewardProfileLine profileLine
    ) {
        return new RewardSettlementLine(
                settlement,
                profileLine.getRewardDefinition(),
                profileLine.getSortOrder(),
                profileLine.effectiveAmount()
        );
    }

    void succeed() {
        if (status == RewardSettlementLineStatus.SUCCEEDED) {
            return;
        }
        if (status == RewardSettlementLineStatus.FAILED) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_LINE_ALREADY_FAILED);
        }
        status = RewardSettlementLineStatus.SUCCEEDED;
        failureCode = null;
    }

    void succeedExp() {
        assertExp();
        succeed();
    }

    public boolean isExpProcessingRequired() {
        assertExp();
        if (status == RewardSettlementLineStatus.FAILED) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_LINE_ALREADY_FAILED);
        }
        return status == RewardSettlementLineStatus.PENDING;
    }

    boolean isPending() {
        return status == RewardSettlementLineStatus.PENDING;
    }

    void fail(ErrorCode errorCode) {
        if (status == RewardSettlementLineStatus.SUCCEEDED) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_SUCCEEDED_LINE_CANNOT_FAIL);
        }
        if (errorCode == null) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_FAILURE_CODE_REQUIRED);
        }
        if (status == RewardSettlementLineStatus.FAILED) {
            return;
        }
        status = RewardSettlementLineStatus.FAILED;
        failureCode = errorCode.code();
    }

    boolean prepareRetry() {
        if (status == RewardSettlementLineStatus.PENDING) {
            return false;
        }
        if (status == RewardSettlementLineStatus.SUCCEEDED) {
            throw new DomainException(
                    RewardError.REWARD_SETTLEMENT_SUCCEEDED_LINE_CANNOT_RETRY
            );
        }
        status = RewardSettlementLineStatus.PENDING;
        failureCode = null;
        return true;
    }

    private void assertExp() {
        if (rewardType != RewardType.EXP) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_LINE_NOT_EXP);
        }
    }

}
