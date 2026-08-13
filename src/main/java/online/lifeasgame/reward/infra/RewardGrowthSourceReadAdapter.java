package online.lifeasgame.reward.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.application.internal.RewardGrowthSourceReadApi;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

import static online.lifeasgame.reward.domain.QRewardSettlement.rewardSettlement;
import static online.lifeasgame.reward.domain.QRewardSettlementLine.rewardSettlementLine;

@Repository
@RequiredArgsConstructor
public class RewardGrowthSourceReadAdapter implements RewardGrowthSourceReadApi {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RewardGrowthSource> findAllByRewardLineIds(Set<Long> rewardLineIds) {
        if (rewardLineIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .select(Projections.constructor(
                        RewardGrowthSource.class,
                        rewardSettlementLine.id,
                        rewardSettlement.sourceType.stringValue(),
                        rewardSettlement.sourceId
                ))
                .from(rewardSettlementLine)
                .join(rewardSettlementLine.settlement, rewardSettlement)
                .where(rewardSettlementLine.id.in(rewardLineIds))
                .fetch();
    }
}
