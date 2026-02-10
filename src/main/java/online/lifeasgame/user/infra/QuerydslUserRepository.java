package online.lifeasgame.user.infra;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.application.query.UserSearchQuery;
import online.lifeasgame.user.domain.UserStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

import static online.lifeasgame.user.domain.QUser.user;

@Repository
@RequiredArgsConstructor
public class QuerydslUserRepository {

    private final JPAQueryFactory queryFactory;


    public UserSearchQuery.SearchResult search(String email, String nickname, UserStatus status, int page, int size) {
        long offset = (long) page * size;

        List<UserSearchQuery.UserRow> content = queryFactory
                .select(
                        Projections.constructor(
                                UserSearchQuery.UserRow.class,
                                user.id,
                                user.email.value,
                                user.nickname.value,
                                user.status.stringValue(),
                                user.createdAt
                        )
                )
                .from(user)
                .where(
                        emailContains(email),
                        nicknameContains(nickname),
                        statusEq(status)
                )
                .orderBy(user.createdAt.desc(), user.id.desc())
                .offset(offset)
                .limit(size)
                .fetch();

        Long total = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        emailContains(email),
                        nicknameContains(nickname),
                        statusEq(status)
                )
                .fetchOne();

        return new UserSearchQuery.SearchResult(content, total == null ? 0L : total);
    }

    private BooleanExpression emailContains(String email) {
        if (email == null || email.isBlank()) return null;
        return user.email.value.containsIgnoreCase(email.trim());
    }

    private BooleanExpression nicknameContains(String nickname) {
        if (nickname == null || nickname.isBlank()) return null;
        return user.nickname.value.containsIgnoreCase(nickname.trim());
    }

    private BooleanExpression statusEq(UserStatus status) {
        if (status == null) return null;
        return user.status.eq(status);
    }
}
