package online.lifeasgame.economy.application.internal;

import java.util.Optional;

public interface RewardGoldCreditApi {
    Receipt credit(Long rewardLineId, Long playerId, Long accountId, long amount);
    Optional<Receipt> find(Long rewardLineId);

    record Receipt(Long rewardLineId, Long playerId, Long accountId, long amount) {}
}
