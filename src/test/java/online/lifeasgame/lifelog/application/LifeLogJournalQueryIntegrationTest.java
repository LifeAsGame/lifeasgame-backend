package online.lifeasgame.lifelog.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
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
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogPeriodKey;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties =
        "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
@DisplayName("LifeLog canonical Journal query")
class LifeLogJournalQueryIntegrationTest {

    private static final Long PLAYER_ID = 256L;
    private static final Long OTHER_PLAYER_ID = 257L;
    private static final Instant NEWEST =
            Instant.parse("2026-08-11T12:00:00Z");
    private static final Instant OLDER =
            Instant.parse("2026-08-10T12:00:00Z");

    @Autowired
    private LifeLogJournalQueryService queryService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    @BeforeEach
    void setUp() {
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willReturn(PLAYER_ID);
    }

    @Nested
    @DisplayName("Journal 목록을 조회할 때")
    class ListJournal {

        @Test
        @DisplayName("canonical record가 없으면 빈 page를 반환한다")
        void returnsEmptyPage() {
            LifeLogJournalResult.Page page = queryService.list(
                    null,
                    null,
                    0,
                    20
            );

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
            assertThat(page.totalPages()).isZero();
        }

        @Test
        @DisplayName("mixed source를 canonical 시각과 ID 역순으로 enrich한다")
        void enrichesMixedSourcesInCanonicalOrder() {
            CollectionLog collection = collection(PLAYER_ID, "책");
            ExerciseLog exercise = exercise(PLAYER_ID);
            MediaLog media = media(PLAYER_ID, "영화");
            LifeLogRecord older = legacy(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    collection.getId(),
                    LifeLogEntryMode.FULL,
                    OLDER
            );
            LifeLogRecord sameTimeFirst = contentReady(
                    PLAYER_ID,
                    LifeLogSourceType.EXERCISE,
                    exercise.getId(),
                    LifeLogSubtype.ACTIVITY,
                    LifeLogEntryMode.FULL,
                    31L,
                    41L,
                    NEWEST
            );
            LifeLogRecord sameTimeLast = contentReady(
                    PLAYER_ID,
                    LifeLogSourceType.MEDIA,
                    media.getId(),
                    LifeLogSubtype.MEMORY,
                    LifeLogEntryMode.QUICK,
                    null,
                    null,
                    NEWEST
            );
            flushAndClear();

            LifeLogJournalResult.Page page = queryService.list(
                    null,
                    null,
                    0,
                    20
            );

            assertThat(page.content())
                    .extracting(LifeLogJournalResult.Entry::lifeLogId)
                    .containsExactly(
                            sameTimeLast.getId(),
                            sameTimeFirst.getId(),
                            older.getId()
                    );
            assertThat(page.content())
                    .extracting(LifeLogJournalResult.Entry::preview)
                    .containsExactly(
                            new LifeLogJournalResult.MediaPreview(
                                    "MOVIE", "영화", 2, 10,
                                    "WATCHING", 4.5
                            ),
                            new LifeLogJournalResult.ExercisePreview(
                                    "RUNNING", 30, 5.0, 200,
                                    LocalDate.of(2026, 8, 11), "아침 달리기"
                            ),
                            new LifeLogJournalResult.CollectionPreview(
                                    "BOOK", "책", 2
                            )
                    );
            LifeLogJournalResult.Entry roleLinked = page.content().get(1);
            assertThat(roleLinked.primaryRoleId()).isEqualTo(31L);
            assertThat(roleLinked.roleEventId()).isEqualTo(41L);
            assertThat(roleLinked.recordedAt()).isEqualTo(NEWEST);
        }

        @Test
        @DisplayName("legacy null metadata를 추론하지 않고 null로 유지한다")
        void preservesLegacyNullMetadata() {
            CollectionLog collection = collection(PLAYER_ID, "기록");
            legacy(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    collection.getId(),
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            flushAndClear();

            LifeLogJournalResult.Entry entry = queryService.list(
                    null,
                    null,
                    0,
                    20
            ).content().getFirst();

            assertThat(entry.subtype()).isNull();
            assertThat(entry.reflectionScope()).isNull();
            assertThat(entry.periodKey()).isNull();
            assertThat(entry.primaryRoleId()).isNull();
            assertThat(entry.roleEventId()).isNull();
        }

        @Test
        @DisplayName("page 경계에서도 canonical stable order를 보존한다")
        void paginatesDeterministically() {
            CollectionLog first = collection(PLAYER_ID, "첫 기록");
            CollectionLog second = collection(PLAYER_ID, "둘째 기록");
            LifeLogRecord firstRecord = legacy(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    first.getId(),
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            LifeLogRecord secondRecord = legacy(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    second.getId(),
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            flushAndClear();

            LifeLogJournalResult.Page page = queryService.list(
                    null,
                    null,
                    1,
                    1
            );

            assertThat(page.content())
                    .extracting(LifeLogJournalResult.Entry::lifeLogId)
                    .containsExactly(firstRecord.getId());
            assertThat(secondRecord.getId()).isGreaterThan(firstRecord.getId());
            assertThat(page.totalElements()).isEqualTo(2);
            assertThat(page.totalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Journal filter를 적용할 때")
    class FilterJournal {

        @Test
        @DisplayName("primaryRoleId와 subtype을 함께 만족한 own record만 반환한다")
        void filtersByRoleAndSubtypeWithinOwnership() {
            CollectionLog matched = collection(PLAYER_ID, "matched");
            CollectionLog wrongSubtype = collection(PLAYER_ID, "wrong subtype");
            CollectionLog wrongRole = collection(PLAYER_ID, "wrong role");
            CollectionLog otherOwner = collection(OTHER_PLAYER_ID, "other");
            LifeLogRecord expected = contentReady(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    matched.getId(),
                    LifeLogSubtype.MEMORY,
                    LifeLogEntryMode.FULL,
                    10L,
                    null,
                    NEWEST
            );
            contentReady(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    wrongSubtype.getId(),
                    LifeLogSubtype.ACTIVITY,
                    LifeLogEntryMode.FULL,
                    10L,
                    null,
                    NEWEST
            );
            contentReady(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    wrongRole.getId(),
                    LifeLogSubtype.MEMORY,
                    LifeLogEntryMode.FULL,
                    20L,
                    null,
                    NEWEST
            );
            contentReady(
                    OTHER_PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    otherOwner.getId(),
                    LifeLogSubtype.MEMORY,
                    LifeLogEntryMode.FULL,
                    10L,
                    null,
                    NEWEST
            );
            flushAndClear();

            LifeLogJournalResult.Page page = queryService.list(
                    10L,
                    "MEMORY",
                    0,
                    20
            );

            assertThat(page.content())
                    .extracting(LifeLogJournalResult.Entry::lifeLogId)
                    .containsExactly(expected.getId());
        }
    }

    @Nested
    @DisplayName("Journal 상세를 조회할 때")
    class ReadDetail {

        @Test
        @DisplayName("Collection Exercise Media와 Quick source를 명시적으로 projection한다")
        void projectsEverySupportedSource() {
            CollectionLog collection = collection(PLAYER_ID, "컬렉션");
            ExerciseLog exercise = exercise(PLAYER_ID);
            MediaLog media = media(PLAYER_ID, "미디어");
            CollectionLog quick = collection(PLAYER_ID, "빠른 기록");
            LifeLogRecord collectionRecord = legacy(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    collection.getId(),
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            LifeLogRecord exerciseRecord = legacy(
                    PLAYER_ID,
                    LifeLogSourceType.EXERCISE,
                    exercise.getId(),
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            LifeLogRecord mediaRecord = legacy(
                    PLAYER_ID,
                    LifeLogSourceType.MEDIA,
                    media.getId(),
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            LifeLogRecord quickRecord = legacy(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    quick.getId(),
                    LifeLogEntryMode.QUICK,
                    NEWEST
            );
            flushAndClear();

            assertThat(queryService.detail(collectionRecord.getId()).source())
                    .isInstanceOfSatisfying(
                            LifeLogJournalResult.CollectionSource.class,
                            source -> assertThat(source.tags())
                                    .containsExactly("owned")
                    );
            assertThat(queryService.detail(exerciseRecord.getId()).source())
                    .isInstanceOf(LifeLogJournalResult.ExerciseSource.class);
            assertThat(queryService.detail(mediaRecord.getId()).source())
                    .isInstanceOfSatisfying(
                            LifeLogJournalResult.MediaSource.class,
                            source -> assertThat(source.tags())
                                    .containsExactly("journal")
                    );
            assertThat(queryService.detail(quickRecord.getId()).entryMode())
                    .isEqualTo("QUICK");
        }

        @Test
        @DisplayName("missing과 cross-owner lifeLog를 같은 404로 닫는다")
        void hidesCanonicalOwnership() {
            CollectionLog other = collection(OTHER_PLAYER_ID, "other");
            LifeLogRecord otherRecord = legacy(
                    OTHER_PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    other.getId(),
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            flushAndClear();

            assertNotFound(999_999L);
            assertNotFound(otherRecord.getId());
        }

        @Test
        @DisplayName("canonical source가 없거나 다른 owner이면 invariant 500으로 실패한다")
        void failsClosedForMissingOrCrossOwnerSource() {
            LifeLogRecord missing = legacy(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    999_991L,
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            CollectionLog other = collection(OTHER_PLAYER_ID, "other source");
            LifeLogRecord crossOwnerSource = legacy(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    other.getId(),
                    LifeLogEntryMode.FULL,
                    OLDER
            );
            flushAndClear();

            assertSourceUnavailable(missing.getId());
            assertSourceUnavailable(crossOwnerSource.getId());
        }
    }

    @Nested
    @DisplayName("mixed preview를 enrich할 때")
    class BatchEnrichment {

        @Test
        @DisplayName("canonical page와 type별 batch를 합쳐 최대 다섯 query만 실행한다")
        void boundsMixedPageQueryCount() {
            CollectionLog collection = collection(PLAYER_ID, "collection");
            ExerciseLog exercise = exercise(PLAYER_ID);
            MediaLog media = media(PLAYER_ID, "media");
            legacy(
                    PLAYER_ID,
                    LifeLogSourceType.COLLECTION,
                    collection.getId(),
                    LifeLogEntryMode.FULL,
                    OLDER
            );
            legacy(
                    PLAYER_ID,
                    LifeLogSourceType.EXERCISE,
                    exercise.getId(),
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            legacy(
                    PLAYER_ID,
                    LifeLogSourceType.MEDIA,
                    media.getId(),
                    LifeLogEntryMode.FULL,
                    NEWEST
            );
            flushAndClear();
            Statistics statistics = entityManagerFactory
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            statistics.clear();

            LifeLogJournalResult.Page page = queryService.list(
                    null,
                    null,
                    0,
                    20
            );

            assertThat(page.content()).hasSize(3);
            assertThat(statistics.getPrepareStatementCount())
                    .isLessThanOrEqualTo(5);
        }
    }

    private CollectionLog collection(Long playerId, String title) {
        CollectionLog log = CollectionLog.create(
                playerId,
                CollectionCategory.BOOK,
                Title.of(title, "original " + title),
                Quantity.of(2),
                "good",
                "store",
                CollectionTags.of(Set.of("owned"))
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
                LocalDate.of(2026, 8, 11),
                "아침 달리기"
        );
        entityManager.persist(log);
        entityManager.flush();
        return log;
    }

    private MediaLog media(Long playerId, String title) {
        MediaLog log = MediaLog.create(
                playerId,
                MediaCategory.MOVIE,
                Title.of(title, "original " + title),
                EpisodeProgress.of(2, 10),
                WatchStatus.WATCHING,
                MediaTags.of(Set.of("journal"))
        );
        log.rate(4.5);
        entityManager.persist(log);
        entityManager.flush();
        return log;
    }

    private LifeLogRecord legacy(
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

    private LifeLogRecord contentReady(
            Long playerId,
            LifeLogSourceType sourceType,
            Long sourceId,
            LifeLogSubtype subtype,
            LifeLogEntryMode entryMode,
            Long primaryRoleId,
            Long roleEventId,
            Instant occurredAt
    ) {
        LifeLogReflectionScope scope = subtype == LifeLogSubtype.REFLECTION
                ? LifeLogReflectionScope.WEEKLY_LOOKBACK
                : null;
        LifeLogPeriodKey periodKey = scope == null
                ? null
                : new LifeLogPeriodKey("2026-W33");
        LifeLogRecord record = LifeLogRecord.contentReady(
                playerId,
                sourceType,
                sourceId,
                subtype,
                entryMode,
                scope,
                periodKey,
                primaryRoleId,
                roleEventId,
                occurredAt
        );
        entityManager.persist(record);
        entityManager.flush();
        return record;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void assertNotFound(Long lifeLogId) {
        assertThatThrownBy(() -> queryService.detail(lifeLogId))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(LifeLogError.LIFE_LOG_NOT_FOUND)
                );
    }

    private void assertSourceUnavailable(Long lifeLogId) {
        assertThatThrownBy(() -> queryService.detail(lifeLogId))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        LifeLogError.LIFE_LOG_SOURCE_UNAVAILABLE
                                )
                );
    }
}
