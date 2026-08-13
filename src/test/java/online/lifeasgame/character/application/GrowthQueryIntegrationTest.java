package online.lifeasgame.character.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.character.application.result.GrowthResult;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.growth.PlayerGrowthChange;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.datasource.url=jdbc:h2:mem:growth-query-264;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
@Transactional
@DisplayName("Growth bounded read integration")
class GrowthQueryIntegrationTest {

    private static final Instant SAME_TIME = Instant.parse("2026-08-13T00:00:00Z");

    @Autowired
    private GrowthQueryService service;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    @Nested
    @DisplayName("current Player의 history를 읽을 때")
    class CurrentPlayerHistory {

        @Test
        @DisplayName("player를 격리하고 같은 timestamp에서는 id DESC로 최근 20개만 반환한다")
        void isolatesPlayerAndBoundsStableOrdering() {
            Player current = persistPlayer(26401L, "current");
            Player other = persistPlayer(26402L, "other");
            given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(current.getId());

            List<Long> currentIds = new ArrayList<>();
            for (int index = 0; index < 22; index++) {
                currentIds.add(persistChange(current.getId(), 10_000L + index, index * 10L).getId());
            }
            Long otherChangeId = persistChange(other.getId(), 20_000L, 0).getId();
            entityManager.flush();
            jdbcTemplate.update(
                    "UPDATE player_growth_changes SET created_at = ?",
                    SAME_TIME
            );
            Statistics statistics = statistics();
            statistics.clear();

            List<GrowthResult.RecentExpChange> result = service
                    .getCurrentGrowth().recentExpChanges();

            assertThat(result).hasSize(20);
            assertThat(result).extracting(GrowthResult.RecentExpChange::changeId)
                    .containsExactlyElementsOf(currentIds.reversed().subList(0, 20))
                    .doesNotContain(otherChangeId, currentIds.get(0), currentIds.get(1));
            assertThat(result).extracting(GrowthResult.RecentExpChange::occurredAt)
                    .containsOnly(SAME_TIME);
            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Reward source를 한 query로 조합하고 전체 read를 세 query 이내로 유지한다")
        void composesProvenanceWithinThreeQueries() {
            Player current = persistPlayer(26403L, "query-budget");
            given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(current.getId());
            Long sourcedLineId = insertQuestCompletionRewardLine(current.getId(), 880L);
            PlayerGrowthChange missing = persistChange(current.getId(), 30_000L, 0);
            PlayerGrowthChange sourced = persistChange(current.getId(), sourcedLineId, 10);
            entityManager.flush();
            Statistics statistics = statistics();
            statistics.clear();

            List<GrowthResult.RecentExpChange> result = service
                    .getCurrentGrowth().recentExpChanges();

            assertThat(result).hasSize(2);
            GrowthResult.RecentExpChange sourcedResult = result.stream()
                    .filter(change -> change.changeId().equals(sourced.getId()))
                    .findFirst()
                    .orElseThrow();
            GrowthResult.RecentExpChange missingResult = result.stream()
                    .filter(change -> change.changeId().equals(missing.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(sourcedResult.sourceType()).isEqualTo("QUEST_COMPLETION");
            assertThat(sourcedResult.sourceId()).isEqualTo(880L);
            assertThat(missingResult.sourceType()).isNull();
            assertThat(missingResult.sourceId()).isNull();
            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
        }
    }

    private Player persistPlayer(Long userId, String name) {
        Player player = Player.linkStart(userId, Name.of(name), GenderType.MALE);
        entityManager.persist(player);
        entityManager.flush();
        return player;
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private PlayerGrowthChange persistChange(Long playerId, Long rewardLineId, long beforeExp) {
        Player.GainResult gain = new Player.GainResult(
                10, 10, 0, 1, 1, beforeExp, beforeExp + 10,
                0, 0, 0, 0
        );
        PlayerGrowthChange change = PlayerGrowthChange.rewardExp(playerId, rewardLineId, gain);
        entityManager.persist(change);
        entityManager.flush();
        return change;
    }

    private Long insertQuestCompletionRewardLine(Long playerId, Long acceptanceId) {
        jdbcTemplate.update("""
                INSERT INTO reward_settlements (
                    player_id, source_type, source_id,
                    reward_profile_id, reward_profile_code, status,
                    created_at, updated_at
                ) VALUES (?, 'QUEST_COMPLETION', ?, 1, 'TEST', 'COMPLETED', ?, ?)
                """, playerId, acceptanceId, SAME_TIME, SAME_TIME);
        Long settlementId = jdbcTemplate.queryForObject(
                "SELECT id FROM reward_settlements WHERE player_id = ? AND source_id = ?",
                Long.class,
                playerId,
                acceptanceId
        );
        jdbcTemplate.update("""
                INSERT INTO reward_settlement_lines (
                    reward_settlement_id, reward_definition_id,
                    reward_definition_code, reward_type, amount,
                    sort_order, status, created_at, updated_at
                ) VALUES (?, 1, 'EXP_TEST', 'EXP', 10, 0, 'SUCCEEDED', ?, ?)
                """, settlementId, SAME_TIME, SAME_TIME);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reward_settlement_lines WHERE reward_settlement_id = ?",
                Long.class,
                settlementId
        );
    }
}
