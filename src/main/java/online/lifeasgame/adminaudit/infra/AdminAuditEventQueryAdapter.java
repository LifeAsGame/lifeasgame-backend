package online.lifeasgame.adminaudit.infra;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.application.query.AdminAuditEventQuery;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static online.lifeasgame.adminaudit.domain.QAdminAuditEvent.adminAuditEvent;

@Repository
@RequiredArgsConstructor
class AdminAuditEventQueryAdapter implements AdminAuditEventQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Row> find(Filter filter, int limit) {
        return queryFactory
                .select(com.querydsl.core.types.Projections.constructor(
                        Row.class,
                        adminAuditEvent.id,
                        adminAuditEvent.actorUserId,
                        adminAuditEvent.action,
                        adminAuditEvent.targetType,
                        adminAuditEvent.targetId,
                        adminAuditEvent.reason,
                        adminAuditEvent.result,
                        adminAuditEvent.correlationId,
                        adminAuditEvent.idempotencyKey,
                        adminAuditEvent.occurredAt
                ))
                .from(adminAuditEvent)
                .where(
                        actorEq(filter),
                        actionEq(filter),
                        targetTypeEq(filter),
                        targetIdEq(filter),
                        resultEq(filter),
                        correlationEq(filter),
                        occurredAtFrom(filter.from()),
                        occurredAtBefore(filter.to()),
                        beforeCursor(filter.cursor())
                )
                .orderBy(
                        adminAuditEvent.occurredAt.desc(),
                        adminAuditEvent.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    private BooleanExpression actorEq(Filter filter) {
        return filter.actorUserId() == null
                ? null
                : adminAuditEvent.actorUserId.eq(filter.actorUserId());
    }

    private BooleanExpression actionEq(Filter filter) {
        return filter.action() == null
                ? null
                : adminAuditEvent.action.eq(filter.action().value());
    }

    private BooleanExpression targetTypeEq(Filter filter) {
        return filter.targetType() == null
                ? null
                : adminAuditEvent.targetType.eq(filter.targetType().value());
    }

    private BooleanExpression targetIdEq(Filter filter) {
        return filter.targetId() == null
                ? null
                : adminAuditEvent.targetId.eq(filter.targetId());
    }

    private BooleanExpression resultEq(Filter filter) {
        return filter.result() == null
                ? null
                : adminAuditEvent.result.eq(filter.result());
    }

    private BooleanExpression correlationEq(Filter filter) {
        return filter.correlationId() == null
                ? null
                : adminAuditEvent.correlationId.eq(filter.correlationId());
    }

    private BooleanExpression occurredAtFrom(Instant from) {
        return from == null ? null : adminAuditEvent.occurredAt.goe(from);
    }

    private BooleanExpression occurredAtBefore(Instant to) {
        return to == null ? null : adminAuditEvent.occurredAt.lt(to);
    }

    private BooleanBuilder beforeCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        return new BooleanBuilder()
                .and(adminAuditEvent.occurredAt.lt(cursor.occurredAt()))
                .or(adminAuditEvent.occurredAt.eq(cursor.occurredAt())
                        .and(adminAuditEvent.id.lt(cursor.id())));
    }
}
