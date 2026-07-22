package online.lifeasgame.reward.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.error.ErrorCode;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.reward.domain.error.RewardError;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@AggregateRoot
@Table(
        name = "reward_settlements",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reward_settlement_source",
                columnNames = {"player_id", "source_type", "source_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardSettlement extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 40, nullable = false)
    private RewardSettlementSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "reward_profile_id", nullable = false)
    private Long rewardProfileId;

    @Column(name = "reward_profile_code", length = 80, nullable = false)
    private String rewardProfileCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RewardSettlementStatus status;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<RewardSettlementLine> lines = new ArrayList<>();

    private RewardSettlement(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId,
            RewardProfile rewardProfile
    ) {
        validateIdentity(playerId, sourceType, sourceId);
        validateProfile(rewardProfile);
        this.playerId = playerId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.rewardProfileId = rewardProfile.getId();
        this.rewardProfileCode = rewardProfile.getCode();
        this.status = RewardSettlementStatus.PENDING;
        rewardProfile.getLines().stream()
                .map(line -> RewardSettlementLine.snapshot(this, line))
                .forEach(lines::add);
    }

    public static RewardSettlement create(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId,
            RewardProfile rewardProfile
    ) {
        return new RewardSettlement(playerId, sourceType, sourceId, rewardProfile);
    }

    public List<RewardSettlementLine> getLines() {
        return List.copyOf(lines);
    }

    public void markLineSucceeded(int sortOrder) {
        findLine(sortOrder).succeed();
        recalculateStatus();
    }

    public void markLineFailed(int sortOrder, ErrorCode errorCode) {
        findLine(sortOrder).fail(errorCode);
        recalculateStatus();
    }

    private RewardSettlementLine findLine(int sortOrder) {
        return lines.stream()
                .filter(line -> line.getSortOrder() == sortOrder)
                .findFirst()
                .orElseThrow(() -> new DomainException(RewardError.REWARD_SETTLEMENT_LINE_NOT_FOUND));
    }

    private void recalculateStatus() {
        if (lines.stream().anyMatch(line -> line.getStatus() == RewardSettlementLineStatus.PENDING)) {
            status = RewardSettlementStatus.PENDING;
            return;
        }
        if (lines.stream().allMatch(line -> line.getStatus() == RewardSettlementLineStatus.SUCCEEDED)) {
            status = RewardSettlementStatus.COMPLETED;
            return;
        }
        if (lines.stream().allMatch(line -> line.getStatus() == RewardSettlementLineStatus.FAILED)) {
            status = RewardSettlementStatus.FAILED;
            return;
        }
        status = RewardSettlementStatus.PARTIAL_FAILED;
    }

    private static void validateIdentity(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId
    ) {
        if (playerId == null || playerId <= 0) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_PLAYER_ID_REQUIRED);
        }
        if (sourceType == null) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_SOURCE_TYPE_REQUIRED);
        }
        if (sourceId == null || sourceId <= 0) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_SOURCE_ID_REQUIRED);
        }
    }

    private static void validateProfile(RewardProfile rewardProfile) {
        if (rewardProfile == null) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_PROFILE_REQUIRED);
        }
        rewardProfile.assertActive();
        if (rewardProfile.getId() == null) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_PROFILE_ID_REQUIRED);
        }
        if (rewardProfile.getLines().isEmpty()) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_PROFILE_LINES_REQUIRED);
        }
    }
}
