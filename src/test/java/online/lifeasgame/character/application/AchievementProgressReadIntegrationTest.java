package online.lifeasgame.character.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.character.application.internal.AchievementProgressReadApi;
import online.lifeasgame.character.application.query.PlayerAchievementQuery;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import online.lifeasgame.character.domain.PlayerAchievement;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
        "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
@DisplayName("Achievement recent read provider")
class AchievementProgressReadIntegrationTest {

    private static final Long PLAYER_ID = 260L;
    private static final Long OTHER_PLAYER_ID = 261L;
    private static final Instant ACQUIRED_AT =
            Instant.parse("2026-08-12T00:00:00Z");

    @Autowired
    private AchievementProgressReadApi achievementProgressReadApi;

    @Autowired
    private PlayerAchievementQuery playerAchievementQuery;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("획득 업적이 없을 때")
    class NoAchievements {

        @Test
        @DisplayName("한 번의 bounded query로 빈 목록을 반환한다")
        void returnsEmptyList() {
            Statistics statistics = statistics();
            statistics.clear();

            List<AchievementProgressReadApi.RecentAchievement> result =
                    achievementProgressReadApi.recentAchievements(
                            PLAYER_ID,
                            5
                    );

            assertThat(result).isEmpty();
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("여러 Player가 업적을 획득했을 때")
    class AcquiredAchievements {

        @Test
        @DisplayName("현재 Player의 최근 다섯 건을 시각과 획득 ID 역순으로 한 query에 반환한다")
        void returnsBoundedOwnedAchievementsInStableOrder() {
            Acquisition recent = acquire(
                    PLAYER_ID, "RECENT", "최근", ACQUIRED_AT.minusSeconds(1)
            );
            Acquisition firstTie = acquire(
                    PLAYER_ID, "TIE_FIRST", "동시 첫째", ACQUIRED_AT
            );
            Acquisition secondTie = acquire(
                    PLAYER_ID, "TIE_SECOND", "동시 둘째", ACQUIRED_AT
            );
            Acquisition older = acquire(
                    PLAYER_ID, "OLDER", "이전", ACQUIRED_AT.minusSeconds(2)
            );
            Acquisition oldestIncluded = acquire(
                    PLAYER_ID, "OLDEST_INCLUDED", "포함 마지막",
                    ACQUIRED_AT.minusSeconds(3)
            );
            Acquisition excluded = acquire(
                    PLAYER_ID, "EXCLUDED", "제외",
                    ACQUIRED_AT.minusSeconds(4)
            );
            acquire(
                    OTHER_PLAYER_ID, "OTHER_PLAYER", "다른 Player",
                    ACQUIRED_AT.plusSeconds(100)
            );
            flushAndClear();
            Statistics statistics = statistics();
            statistics.clear();

            List<AchievementProgressReadApi.RecentAchievement> result =
                    achievementProgressReadApi.recentAchievements(
                            PLAYER_ID,
                            5
                    );

            assertThat(secondTie.playerAchievementId())
                    .isGreaterThan(firstTie.playerAchievementId());
            assertThat(result).extracting(
                    AchievementProgressReadApi.RecentAchievement::achievementId
            ).containsExactly(
                    secondTie.achievementId(),
                    firstTie.achievementId(),
                    recent.achievementId(),
                    older.achievementId(),
                    oldestIncluded.achievementId()
            ).doesNotContain(excluded.achievementId());
            assertThat(result.getFirst()).isEqualTo(
                    new AchievementProgressReadApi.RecentAchievement(
                            secondTie.achievementId(),
                            "TIE_SECOND",
                            "동시 둘째",
                            "STORY",
                            "TIE_SECOND 설명",
                            ACQUIRED_AT
                    )
            );
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);

            List<PlayerAchievementView> existingList =
                    playerAchievementQuery.findViewsByPlayerId(PLAYER_ID);

            assertThat(existingList).hasSize(6);
            assertThat(existingList).extracting(
                    PlayerAchievementView::getAchievementId
            ).containsExactly(
                    secondTie.achievementId(),
                    firstTie.achievementId(),
                    recent.achievementId(),
                    older.achievementId(),
                    oldestIncluded.achievementId(),
                    excluded.achievementId()
            );
        }
    }

    private Acquisition acquire(
            Long playerId,
            String code,
            String name,
            Instant acquiredAt
    ) {
        Achievement achievement = Achievement.create(
                code,
                name,
                AchievementCategory.STORY,
                code + " 설명"
        );
        entityManager.persist(achievement);
        entityManager.flush();
        PlayerAchievement playerAchievement = PlayerAchievement.create(
                playerId,
                achievement.getId()
        );
        entityManager.persist(playerAchievement);
        entityManager.flush();
        jdbcTemplate.update(
                "UPDATE player_achievements SET acquired_at = ? WHERE id = ?",
                Timestamp.from(acquiredAt),
                playerAchievement.getId()
        );
        return new Acquisition(
                playerAchievement.getId(),
                achievement.getId()
        );
    }

    private Statistics statistics() {
        return entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private record Acquisition(
            Long playerAchievementId,
            Long achievementId
    ) {
    }
}
