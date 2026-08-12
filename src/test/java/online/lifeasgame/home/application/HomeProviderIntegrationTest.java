package online.lifeasgame.home.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.home.application.result.HomeResult;
import online.lifeasgame.lifelog.domain.CollectionCategory;
import online.lifeasgame.lifelog.domain.CollectionLog;
import online.lifeasgame.lifelog.domain.CollectionTags;
import online.lifeasgame.lifelog.domain.EpisodeProgress;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.ExerciseMetrics;
import online.lifeasgame.lifelog.domain.MediaCategory;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.MediaTags;
import online.lifeasgame.lifelog.domain.Quantity;
import online.lifeasgame.lifelog.domain.Title;
import online.lifeasgame.lifelog.domain.WatchStatus;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.quest.domain.PlayerQuestRoute;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestCategory;
import online.lifeasgame.quest.domain.QuestRepeatRule;
import online.lifeasgame.quest.domain.QuestReward;
import online.lifeasgame.quest.domain.QuestTarget;
import online.lifeasgame.quest.domain.QuestTargetType;
import online.lifeasgame.quest.domain.QuestTitle;
import online.lifeasgame.quest.domain.RewardStats;
import online.lifeasgame.quest.domain.TimePeriod;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleType;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties =
        "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
@DisplayName("Home provider read integration")
class HomeProviderIntegrationTest {

    private static final Long PLAYER_ID = 258L;
    private static final Long OTHER_PLAYER_ID = 259L;
    private static final Instant NOW =
            Instant.parse("2026-08-12T00:00:00Z");
    private static final Instant WINDOW_START =
            Instant.parse("2026-07-13T00:00:00Z");

    @Autowired
    private HomeQueryService queryService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willReturn(PLAYER_ID);
        given(clock.instant()).willReturn(NOW);
    }

    @Nested
    @DisplayName("기록이 없을 때")
    class EmptyWorld {

        @Test
        @DisplayName("모든 provider가 빈 section을 반환한다")
        void returnsEmptySections() {
            HomeResult.Summary result = queryService.home();

            assertThat(result.recentJournal()).isEmpty();
            assertThat(result.recentAchievements()).isEmpty();
            assertThat(result.journey().currentQuests()).isEmpty();
            assertThat(result.journey().selectedRoutes()).isEmpty();
            assertThat(result.roleActivity30d().totalRecords()).isZero();
            assertThat(result.roleActivity30d().roles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("최근 활동이 있을 때")
    class PopulatedWorld {

        @Test
        @DisplayName("canonical Journal을 다섯 개로 제한하고 mixed preview와 QUICK/null metadata를 보존한다")
        void returnsBoundedCanonicalJournal() {
            CollectionLog collection = collection(PLAYER_ID, "컬렉션");
            ExerciseLog exercise = exercise(PLAYER_ID);
            MediaLog media = media(PLAYER_ID, "미디어");
            CollectionLog oldOne = collection(PLAYER_ID, "오래된 1");
            CollectionLog oldTwo = collection(PLAYER_ID, "오래된 2");
            CollectionLog excluded = collection(PLAYER_ID, "제외");
            LifeLogRecord quick = record(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    collection.getId(),
                    LifeLogEntryMode.QUICK,
                    NOW.minusSeconds(1)
            );
            LifeLogRecord mediaRecord = record(
                    PLAYER_ID,
                    LifeLogSourceType.MEDIA,
                    media.getId(),
                    LifeLogEntryMode.FULL,
                    NOW.minusSeconds(2)
            );
            LifeLogRecord exerciseRecord = record(
                    PLAYER_ID,
                    LifeLogSourceType.EXERCISE,
                    exercise.getId(),
                    LifeLogEntryMode.FULL,
                    NOW.minusSeconds(2)
            );
            LifeLogRecord oldOneRecord = record(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    oldOne.getId(),
                    LifeLogEntryMode.FULL,
                    NOW.minusSeconds(4)
            );
            LifeLogRecord oldTwoRecord = record(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    oldTwo.getId(),
                    LifeLogEntryMode.FULL,
                    NOW.minusSeconds(5)
            );
            LifeLogRecord sixth = record(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    excluded.getId(),
                    LifeLogEntryMode.FULL,
                    NOW.minusSeconds(6)
            );
            flushAndClear();

            List<online.lifeasgame.lifelog.application.result.LifeLogJournalResult.Entry>
                    entries = queryService.home().recentJournal();

            assertThat(entries).hasSize(5);
            assertThat(entries).extracting(
                    online.lifeasgame.lifelog.application.result.LifeLogJournalResult.Entry::lifeLogId
            ).containsExactly(
                    quick.getId(),
                    exerciseRecord.getId(),
                    mediaRecord.getId(),
                    oldOneRecord.getId(),
                    oldTwoRecord.getId()
            ).doesNotContain(sixth.getId());
            assertThat(entries.getFirst().entryMode()).isEqualTo("QUICK");
            assertThat(entries.getFirst().sourceType()).isEqualTo("COLLECTION");
            assertThat(entries.getFirst().subtype()).isNull();
            assertThat(entries).extracting(
                            entry -> entry.preview().getClass().getSimpleName()
                    )
                    .contains(
                            "CollectionPreview",
                            "ExercisePreview",
                            "MediaPreview"
                    );
        }

        @Test
        @DisplayName("GOAL_REACHED와 IN_PROGRESS만 status/시간/ID 순으로 반환한다")
        void returnsOnlyCurrentQuestsInStableOrder() {
            Quest goalOlder = quest("Q_GOAL_OLDER", "목표 도달 이전");
            Quest goalNewer = quest("Q_GOAL_NEWER", "목표 도달 최근");
            Quest inProgress = quest("Q_PROGRESS", "진행 중");
            Quest completed = quest("Q_COMPLETED", "완료");
            Quest canceled = quest("Q_CANCELED", "취소");
            QuestAcceptance olderGoal = acceptance(
                    goalOlder,
                    NOW.minusSeconds(300)
            );
            olderGoal.reachGoal(NOW.minusSeconds(100));
            QuestAcceptance newerGoal = acceptance(
                    goalNewer,
                    NOW.minusSeconds(200)
            );
            newerGoal.reachGoal(NOW.minusSeconds(50));
            acceptance(inProgress, NOW.minusSeconds(10));
            QuestAcceptance done = acceptance(completed, NOW.minusSeconds(5));
            done.reachGoal(NOW.minusSeconds(4));
            done.complete(NOW.minusSeconds(3));
            QuestAcceptance stopped = acceptance(canceled, NOW.minusSeconds(1));
            stopped.cancel();
            acceptance(quest("Q_OTHER", "다른 사용자"), NOW, OTHER_PLAYER_ID);
            flushAndClear();

            HomeResult.Journey journey = queryService.home().journey();
            List<String> statuses = journey
                    .currentQuests().stream()
                    .map(online.lifeasgame.quest.application.internal.QuestProgressReadApi.CurrentQuest::status)
                    .toList();
            List<String> codes = journey
                    .currentQuests().stream()
                    .map(online.lifeasgame.quest.application.internal.QuestProgressReadApi.CurrentQuest::questCode)
                    .toList();

            assertThat(statuses).containsExactly(
                    "GOAL_REACHED",
                    "GOAL_REACHED",
                    "IN_PROGRESS"
            );
            assertThat(codes).containsExactly(
                    "Q_GOAL_NEWER",
                    "Q_GOAL_OLDER",
                    "Q_PROGRESS"
            );
        }

        @Test
        @DisplayName("여러 selected Route를 상태와 선택 시각 순으로 보존한다")
        void preservesMultipleSelectedRoutes() {
            Long first = route("ROUTE_FIRST", "첫 경로");
            Long second = route("ROUTE_SECOND", "둘째 경로");
            Long completedRoute = route("ROUTE_DONE", "완료 경로");
            PlayerQuestRoute older = playerRoute(
                    first,
                    NOW.minusSeconds(300)
            );
            PlayerQuestRoute newer = playerRoute(
                    second,
                    NOW.minusSeconds(100)
            );
            PlayerQuestRoute done = playerRoute(
                    completedRoute,
                    NOW.minusSeconds(10)
            );
            done.complete(done.getCurrentStepId(), NOW.minusSeconds(1));
            flushAndClear();

            List<online.lifeasgame.quest.application.internal.QuestProgressReadApi.SelectedRoute>
                    routes = queryService.home().journey().selectedRoutes();

            assertThat(routes).extracting(
                    online.lifeasgame.quest.application.internal.QuestProgressReadApi.SelectedRoute::routeCode
            ).containsExactly("ROUTE_SECOND", "ROUTE_FIRST", "ROUTE_DONE");
            assertThat(routes).extracting(
                    online.lifeasgame.quest.application.internal.QuestProgressReadApi.SelectedRoute::status
            ).containsExactly("IN_PROGRESS", "IN_PROGRESS", "COMPLETED");
            assertThat(older.getId()).isNotEqualTo(newer.getId());
        }

        @Test
        @DisplayName("[start,end] primaryRoleId만 grouping하고 archived/missing 이름도 count를 보존한다")
        void aggregatesInclusiveRoleWindow() {
            Role active = role("활성", false);
            Role archived = role("보관", true);
            CollectionLog atStart = collection(PLAYER_ID, "start");
            CollectionLog atEnd = collection(PLAYER_ID, "end");
            CollectionLog archivedLog = collection(PLAYER_ID, "archived");
            CollectionLog missingRole = collection(PLAYER_ID, "missing");
            CollectionLog unassigned = collection(PLAYER_ID, "none");
            CollectionLog before = collection(PLAYER_ID, "before");
            CollectionLog after = collection(PLAYER_ID, "after");
            roleRecord(atStart, active.getId(), WINDOW_START);
            roleRecord(atEnd, active.getId(), NOW);
            roleRecord(archivedLog, archived.getId(), NOW.minusSeconds(1));
            roleRecord(missingRole, 999_999L, NOW.minusSeconds(2));
            record(PLAYER_ID, LifeLogSourceType.COLLECTION,
                    unassigned.getId(), LifeLogEntryMode.FULL,
                    NOW.minusSeconds(3));
            roleRecord(before, active.getId(), WINDOW_START.minusSeconds(1));
            roleRecord(after, active.getId(), NOW.plusSeconds(1));
            flushAndClear();

            HomeResult.RoleActivity activity =
                    queryService.home().roleActivity30d();

            assertThat(activity.totalRecords()).isEqualTo(5);
            assertThat(activity.assignedRecords()).isEqualTo(4);
            assertThat(activity.unassignedRecords()).isEqualTo(1);
            assertThat(activity.roles()).containsExactly(
                    new HomeResult.RoleBucket(
                            active.getId(), "활성", 2, 0.5
                    ),
                    new HomeResult.RoleBucket(
                            archived.getId(), "보관", 1, 0.25
                    ),
                    new HomeResult.RoleBucket(
                            999_999L, null, 1, 0.25
                    )
            );
        }

        @Test
        @DisplayName("Achievement를 포함한 mixed world summary를 최대 아홉 query로 읽는다")
        void boundsComposedQueryCount() {
            Role role = role("역할", false);
            CollectionLog collection = collection(PLAYER_ID, "컬렉션");
            ExerciseLog exercise = exercise(PLAYER_ID);
            MediaLog media = media(PLAYER_ID, "미디어");
            roleRecord(collection, role.getId(), NOW.minusSeconds(1));
            record(PLAYER_ID, LifeLogSourceType.EXERCISE, exercise.getId(),
                    LifeLogEntryMode.FULL, NOW.minusSeconds(2));
            record(PLAYER_ID, LifeLogSourceType.MEDIA, media.getId(),
                    LifeLogEntryMode.FULL, NOW.minusSeconds(3));
            acceptance(quest("Q_QUERY", "쿼리 퀘스트"), NOW.minusSeconds(4));
            playerRoute(route("ROUTE_QUERY", "쿼리 경로"),
                    NOW.minusSeconds(5));
            acquire("HOME_QUERY", "Home 조회", PLAYER_ID);
            flushAndClear();
            Statistics statistics = entityManagerFactory
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            statistics.clear();

            HomeResult.Summary result = queryService.home();

            assertThat(result.recentJournal()).hasSize(3);
            assertThat(result.recentAchievements()).hasSize(1);
            assertThat(statistics.getPrepareStatementCount())
                    .isLessThanOrEqualTo(9);
        }
    }

    private CollectionLog collection(Long playerId, String title) {
        CollectionLog log = CollectionLog.create(
                playerId,
                CollectionCategory.BOOK,
                Title.of(title, null),
                Quantity.of(1),
                null,
                null,
                CollectionTags.of(Set.of())
        );
        entityManager.persist(log);
        entityManager.flush();
        return log;
    }

    private ExerciseLog exercise(Long playerId) {
        ExerciseLog log = ExerciseLog.create(
                playerId,
                ExerciseCategory.RUNNING,
                ExerciseMetrics.of(30, 5.0, 200),
                LocalDate.of(2026, 8, 12),
                "운동"
        );
        entityManager.persist(log);
        entityManager.flush();
        return log;
    }

    private MediaLog media(Long playerId, String title) {
        MediaLog log = MediaLog.create(
                playerId,
                MediaCategory.MOVIE,
                Title.of(title, null),
                EpisodeProgress.of(1, 2),
                WatchStatus.WATCHING,
                MediaTags.of(Set.of())
        );
        entityManager.persist(log);
        entityManager.flush();
        return log;
    }

    private LifeLogRecord record(
            Long playerId,
            LifeLogSourceType sourceType,
            Long sourceId,
            LifeLogEntryMode entryMode,
            Instant occurredAt
    ) {
        LifeLogRecord record = LifeLogRecord.legacy(
                playerId,
                sourceType,
                sourceId,
                entryMode,
                occurredAt
        );
        entityManager.persist(record);
        entityManager.flush();
        return record;
    }

    private LifeLogRecord roleRecord(
            CollectionLog source,
            Long roleId,
            Instant occurredAt
    ) {
        LifeLogRecord record = LifeLogRecord.contentReady(
                PLAYER_ID,
                LifeLogSourceType.COLLECTION,
                source.getId(),
                LifeLogSubtype.ACTIVITY,
                LifeLogEntryMode.FULL,
                null,
                null,
                roleId,
                null,
                occurredAt
        );
        entityManager.persist(record);
        entityManager.flush();
        return record;
    }

    private Quest quest(String code, String title) {
        Quest quest = Quest.create(
                code,
                QuestCategory.MAIN,
                QuestTitle.of(title),
                null,
                QuestTarget.of(QuestTargetType.COUNT, 3),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.NONE,
                null
        );
        entityManager.persist(quest);
        entityManager.flush();
        return quest;
    }

    private void acquire(
            String code,
            String name,
            Long playerId
    ) {
        jdbcTemplate.update("""
                INSERT INTO achievements (
                    created_at,
                    updated_at,
                    code,
                    name,
                    category,
                    desc_md
                ) VALUES (?, ?, ?, ?, 'STORY', ?)
                """, NOW, NOW, code, name, "Home feed");
        Long achievementId = jdbcTemplate.queryForObject(
                "SELECT id FROM achievements WHERE code = ?",
                Long.class,
                code
        );
        jdbcTemplate.update("""
                INSERT INTO player_achievements (
                    achievement_id,
                    acquired_at,
                    created_at,
                    player_id,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
                achievementId,
                NOW,
                NOW,
                playerId,
                NOW
        );
    }

    private QuestAcceptance acceptance(Quest quest, Instant acceptedAt) {
        return acceptance(quest, acceptedAt, PLAYER_ID);
    }

    private QuestAcceptance acceptance(
            Quest quest,
            Instant acceptedAt,
            Long playerId
    ) {
        QuestAcceptance acceptance = QuestAcceptance.start(
                quest.getId(),
                playerId,
                TimePeriod.forever(),
                acceptedAt,
                null
        );
        entityManager.persist(acceptance);
        entityManager.flush();
        return acceptance;
    }

    private Long route(String code, String title) {
        jdbcTemplate.update("""
                INSERT INTO quest_routes (
                    code,
                    definition_version,
                    title,
                    description,
                    primary_role_template_code,
                    created_at,
                    updated_at
                ) VALUES (?, 1, ?, NULL, NULL, ?, ?)
                """, code, title, NOW, NOW);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM quest_routes WHERE code = ?",
                Long.class,
                code
        );
    }

    private PlayerQuestRoute playerRoute(
            Long routeId,
            Instant selectedAt
    ) {
        PlayerQuestRoute playerRoute = PlayerQuestRoute.start(
                PLAYER_ID,
                routeId,
                routeId + 10_000,
                selectedAt
        );
        entityManager.persist(playerRoute);
        entityManager.flush();
        return playerRoute;
    }

    private Role role(String name, boolean archived) {
        Role role = Role.create(
                PLAYER_ID,
                RoleType.of("SELF"),
                name,
                null
        );
        if (archived) {
            role.archive();
        }
        entityManager.persist(role);
        entityManager.flush();
        return role;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}
