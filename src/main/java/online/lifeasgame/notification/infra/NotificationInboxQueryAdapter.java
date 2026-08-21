package online.lifeasgame.notification.infra;

import static online.lifeasgame.notification.domain.QPlayerNotification.playerNotification;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.notification.application.query.NotificationInboxQuery;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationInboxQueryAdapter implements NotificationInboxQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Row> findInbox(Long playerId, Long cursor, int limit) {
        return queryFactory
                .select(Projections.constructor(
                        Row.class,
                        playerNotification.id,
                        playerNotification.type,
                        playerNotification.title,
                        playerNotification.body,
                        playerNotification.occurredAt,
                        playerNotification.readAt
                ))
                .from(playerNotification)
                .where(
                        playerNotification.playerId.eq(playerId),
                        before(cursor)
                )
                .orderBy(playerNotification.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public long countUnread(Long playerId) {
        Long count = queryFactory
                .select(playerNotification.count())
                .from(playerNotification)
                .where(
                        playerNotification.playerId.eq(playerId),
                        playerNotification.readAt.isNull()
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanExpression before(Long cursor) {
        return cursor == null ? null : playerNotification.id.lt(cursor);
    }
}
