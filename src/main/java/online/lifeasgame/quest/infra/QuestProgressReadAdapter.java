package online.lifeasgame.quest.infra;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.application.internal.QuestProgressReadApi;
import online.lifeasgame.quest.domain.PlayerQuestRouteStatus;
import online.lifeasgame.quest.domain.QuestStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

import static online.lifeasgame.quest.domain.QPlayerQuestRoute.playerQuestRoute;
import static online.lifeasgame.quest.domain.QQuest.quest;
import static online.lifeasgame.quest.domain.QQuestAcceptance.questAcceptance;
import static online.lifeasgame.quest.domain.QQuestRoute.questRoute;

@Repository
@RequiredArgsConstructor
public class QuestProgressReadAdapter implements QuestProgressReadApi {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CurrentQuest> currentQuests(Long playerId, int limit) {
        NumberExpression<Integer> statusOrder = new CaseBuilder()
                .when(questAcceptance.status.eq(QuestStatus.GOAL_REACHED))
                .then(0)
                .otherwise(1);
        return queryFactory
                .select(
                        questAcceptance.id,
                        quest.code,
                        quest.title.value,
                        questAcceptance.status,
                        questAcceptance.progressValue,
                        quest.target.value,
                        questAcceptance.acceptedAt,
                        questAcceptance.goalReachedAt
                )
                .from(questAcceptance)
                .join(quest).on(quest.id.eq(questAcceptance.questId))
                .where(
                        questAcceptance.playerId.eq(playerId),
                        questAcceptance.status.in(
                                QuestStatus.IN_PROGRESS,
                                QuestStatus.GOAL_REACHED
                        )
                )
                .orderBy(
                        statusOrder.asc(),
                        questAcceptance.acceptedAt.desc(),
                        questAcceptance.id.desc()
                )
                .limit(limit)
                .fetch()
                .stream()
                .map(this::currentQuest)
                .toList();
    }

    @Override
    public List<SelectedRoute> selectedRoutes(Long playerId, int limit) {
        NumberExpression<Integer> statusOrder = new CaseBuilder()
                .when(playerQuestRoute.status.eq(
                        PlayerQuestRouteStatus.IN_PROGRESS
                ))
                .then(0)
                .otherwise(1);
        return queryFactory
                .select(
                        questRoute.id,
                        questRoute.code,
                        questRoute.title,
                        playerQuestRoute.status,
                        playerQuestRoute.currentStepId,
                        playerQuestRoute.selectedAt,
                        playerQuestRoute.completedAt
                )
                .from(playerQuestRoute)
                .join(questRoute).on(
                        questRoute.id.eq(playerQuestRoute.routeId)
                )
                .where(playerQuestRoute.playerId.eq(playerId))
                .orderBy(
                        statusOrder.asc(),
                        playerQuestRoute.selectedAt.desc(),
                        questRoute.id.asc()
                )
                .limit(limit)
                .fetch()
                .stream()
                .map(this::selectedRoute)
                .toList();
    }

    private CurrentQuest currentQuest(Tuple row) {
        QuestStatus status = row.get(questAcceptance.status);
        return new CurrentQuest(
                row.get(questAcceptance.id),
                row.get(quest.code),
                row.get(quest.title.value),
                status.name(),
                row.get(questAcceptance.progressValue),
                row.get(quest.target.value),
                row.get(questAcceptance.acceptedAt),
                row.get(questAcceptance.goalReachedAt)
        );
    }

    private SelectedRoute selectedRoute(Tuple row) {
        PlayerQuestRouteStatus status = row.get(playerQuestRoute.status);
        return new SelectedRoute(
                row.get(questRoute.id),
                row.get(questRoute.code),
                row.get(questRoute.title),
                status.name(),
                row.get(playerQuestRoute.currentStepId),
                row.get(playerQuestRoute.selectedAt),
                row.get(playerQuestRoute.completedAt)
        );
    }
}
