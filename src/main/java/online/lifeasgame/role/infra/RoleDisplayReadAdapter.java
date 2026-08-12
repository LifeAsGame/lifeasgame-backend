package online.lifeasgame.role.infra;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.application.internal.RoleDisplayReadApi;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static online.lifeasgame.role.domain.QRole.role;

@Repository
@RequiredArgsConstructor
public class RoleDisplayReadAdapter implements RoleDisplayReadApi {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, String> findNames(
            Long playerId,
            Collection<Long> roleIds
    ) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return queryFactory
                .select(role.id, role.name)
                .from(role)
                .where(
                        role.playerId.eq(playerId),
                        role.id.in(roleIds)
                )
                .fetch()
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        row -> row.get(role.id),
                        row -> row.get(role.name)
                ));
    }
}
