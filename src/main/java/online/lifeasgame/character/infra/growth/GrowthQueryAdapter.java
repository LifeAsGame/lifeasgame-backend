package online.lifeasgame.character.infra.growth;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.GrowthQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

import static online.lifeasgame.character.domain.growth.QPlayerGrowthChange.playerGrowthChange;

@Repository
@RequiredArgsConstructor
public class GrowthQueryAdapter implements GrowthQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RecentExpChange> findRecentExpChanges(Long playerId, int limit) {
        return queryFactory
                .select(Projections.constructor(
                        RecentExpChange.class,
                        playerGrowthChange.id,
                        playerGrowthChange.rewardLineId,
                        playerGrowthChange.requestedExp,
                        playerGrowthChange.appliedExp,
                        playerGrowthChange.leftoverExp,
                        playerGrowthChange.beforeLevel,
                        playerGrowthChange.afterLevel,
                        playerGrowthChange.beforeTotalExp,
                        playerGrowthChange.afterTotalExp,
                        playerGrowthChange.createdAt
                ))
                .from(playerGrowthChange)
                .where(playerGrowthChange.playerId.eq(playerId))
                .orderBy(
                        playerGrowthChange.createdAt.desc(),
                        playerGrowthChange.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}
