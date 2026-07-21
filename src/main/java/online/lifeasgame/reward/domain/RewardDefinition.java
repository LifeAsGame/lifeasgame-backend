package online.lifeasgame.reward.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.reward.domain.error.RewardError;

@Getter
@Entity
@AggregateRoot
@Table(name = "reward_definitions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardDefinition extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80, nullable = false, unique = true)
    private String code;

    @Column(length = 120, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", length = 20, nullable = false)
    private RewardType rewardType;

    @Column
    private Long amount;

    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false)
    private boolean active;

    private RewardDefinition(
            String code,
            String name,
            RewardType rewardType,
            Long amount,
            Long itemId,
            boolean active
    ) {
        this.code = normalizeRequired(
                code,
                80,
                RewardError.REWARD_DEFINITION_CODE_REQUIRED,
                RewardError.REWARD_DEFINITION_CODE_TOO_LONG
        );
        this.name = normalizeRequired(
                name,
                120,
                RewardError.REWARD_DEFINITION_NAME_REQUIRED,
                RewardError.REWARD_DEFINITION_NAME_TOO_LONG
        );
        if (rewardType == null) {
            throw new DomainException(RewardError.REWARD_LINE_TYPE_REQUIRED);
        }
        this.rewardType = rewardType;
        validatePayload(rewardType, amount, itemId);
        this.amount = amount;
        this.itemId = itemId;
        this.active = active;
    }

    public static RewardDefinition create(
            String code,
            String name,
            RewardType rewardType,
            Long amount,
            Long itemId,
            boolean active
    ) {
        return new RewardDefinition(code, name, rewardType, amount, itemId, active);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String normalizeRequired(
            String value,
            int maxLength,
            RewardError requiredError,
            RewardError tooLongError
    ) {
        if (value == null || value.isBlank()) {
            throw new DomainException(requiredError);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new DomainException(tooLongError);
        }
        return normalized;
    }

    private static void validatePayload(RewardType rewardType, Long amount, Long itemId) {
        if (rewardType == RewardType.EXP) {
            if (amount == null || amount <= 0) {
                throw new DomainException(RewardError.REWARD_AMOUNT_MUST_BE_POSITIVE);
            }
            if (itemId != null) {
                throw new DomainException(RewardError.REWARD_EXP_ITEM_ID_NOT_ALLOWED);
            }
            return;
        }
        if (amount == null || amount <= 0) {
            throw new DomainException(RewardError.REWARD_ITEM_QUANTITY_MUST_BE_POSITIVE);
        }
        if (itemId == null) {
            throw new DomainException(RewardError.REWARD_ITEM_ID_REQUIRED);
        }
        if (itemId <= 0) {
            throw new DomainException(RewardError.REWARD_ITEM_ID_MUST_BE_POSITIVE);
        }
    }
}
