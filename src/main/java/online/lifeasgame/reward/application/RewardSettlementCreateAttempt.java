package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.reward.infra.AccountRewardEntitlementStore;
import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RewardSettlementCreateAttempt {

    private final PlayerLookupApi playerLookupApi;
    private final AccountRewardEntitlementStore entitlements;
    private final RewardProfileReader profileReader;
    private final RewardSettlementWriter settlementWriter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RewardSettlement create(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId,
            String rewardProfileCode
    ) {
        RewardProfile profile = profileReader.getActiveByCodeOrThrow(rewardProfileCode);
        RewardSettlement settlement = RewardSettlement.create(playerId, sourceType, sourceId, profile);
        if (profile.getEntitlementCode() != null) {
            settlement.assignAccount(playerLookupApi.findUserIdByPlayerId(playerId));
        }
        settlementWriter.saveAndFlush(settlement);
        if (profile.getEntitlementCode() != null && !entitlements.claim(
                settlement.getAccountId(), profile.getEntitlementCode(), settlement.getId())) {
            settlement.denyAlreadyUsedEntitlement();
            settlementWriter.saveAndFlush(settlement);
        }
        return settlement;
    }
}
