package online.lifeasgame.character.infra;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerConnectionReadApi;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static online.lifeasgame.character.domain.QPlayer.player;

@Repository
@RequiredArgsConstructor
public class PlayerConnectionReadAdapter implements PlayerConnectionReadApi {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, PlayerSummary> findAllByPlayerIds(Set<Long> playerIds) {
        if (playerIds.isEmpty()) {
            return Map.of();
        }
        return queryFactory
                .select(
                        player.id,
                        player.name.value,
                        player.job,
                        player.level.value
                )
                .from(player)
                .where(player.id.in(playerIds))
                .fetch()
                .stream()
                .map(row -> new PlayerSummary(
                        row.get(player.id),
                        row.get(player.name.value),
                        row.get(player.job),
                        row.get(player.level.value)
                ))
                .collect(Collectors.toUnmodifiableMap(
                        PlayerSummary::playerId,
                        Function.identity()
                ));
    }
}
