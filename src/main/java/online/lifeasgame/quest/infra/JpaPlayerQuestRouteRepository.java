package online.lifeasgame.quest.infra;

import online.lifeasgame.quest.domain.PlayerQuestRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JpaPlayerQuestRouteRepository
        extends JpaRepository<PlayerQuestRoute, Long> {

    Optional<PlayerQuestRoute> findByPlayerIdAndRouteId(
            Long playerId,
            Long routeId
    );

    @Query(value = """
            SELECT *
            FROM player_quest_routes
            WHERE player_id = :playerId
              AND route_id = :routeId
            FOR UPDATE
            """, nativeQuery = true)
    Optional<PlayerQuestRoute> findByPlayerIdAndRouteIdForUpdate(
            @Param("playerId") Long playerId,
            @Param("routeId") Long routeId
    );

    List<PlayerQuestRoute> findAllByPlayerId(Long playerId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO player_quest_routes (
                player_id,
                route_id,
                current_step_id,
                status,
                selected_at,
                completed_at,
                version,
                created_at,
                updated_at
            ) VALUES (
                :playerId,
                :routeId,
                :firstStepId,
                'IN_PROGRESS',
                :selectedAt,
                NULL,
                0,
                :selectedAt,
                :selectedAt
            )
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("playerId") Long playerId,
            @Param("routeId") Long routeId,
            @Param("firstStepId") Long firstStepId,
            @Param("selectedAt") Instant selectedAt
    );
}
