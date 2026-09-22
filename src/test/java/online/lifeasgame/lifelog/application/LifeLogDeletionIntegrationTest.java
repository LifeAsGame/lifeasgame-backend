package online.lifeasgame.lifelog.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.home.application.HomeQueryService;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.repository.LifeLogRecordRepository;
import online.lifeasgame.role.application.RoleService;
import online.lifeasgame.role.application.command.RoleCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("원본 삭제와 Journal의 MySQL 수명주기")
class LifeLogDeletionIntegrationTest {
    private static final Long PLAYER = 359001L;
    private static final Long OTHER = 359002L;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired CollectionLogService collections;
    @Autowired MediaLogService media;
    @Autowired ExerciseLogService exercises;
    @Autowired LifeLogJournalQueryService journal;
    @Autowired HomeQueryService home;
    @Autowired RoleService roles;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @MockitoBean CurrentPlayerAccessor currentPlayer;
    @MockitoSpyBean LifeLogRecordRepository records;
    private Long roleId;

    @BeforeEach
    void setUp() {
        reset(records);
        for (String table : List.of("collection_log_tags", "media_log_tags", "life_log_records",
                "collection_logs", "media_logs", "exercise_logs", "outbox_events")) {
            jdbc.update("DELETE FROM " + table);
        }
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(PLAYER);
        roleId = roles.create(new RoleCommand.Create("PERSONAL", "기록", null)).id();
    }

    @ParameterizedTest
    @EnumSource(LifeLogSourceType.class)
    @DisplayName("정상 삭제와 반복 삭제는 목록·상세·총계·최근·Role 집계에 함께 반영되고 타인 데이터는 보존된다")
    void deletesOwnedSourceAndJournal(LifeLogSourceType type) {
        Long oldest = create(type, PLAYER);
        Long middle = create(type, PLAYER);
        Long newest = create(type, PLAYER);
        // A different source type must survive even if its numeric source ID overlaps.
        LifeLogSourceType sibling = type == LifeLogSourceType.MEDIA
                ? LifeLogSourceType.COLLECTION : LifeLogSourceType.MEDIA;
        Long siblingId = create(sibling, PLAYER);
        Long recordId = recordId(type, middle);
        Long foreign = create(type, OTHER);
        int outboxCount = count("outbox_events");

        delete(type, OTHER, middle);
        delete(type, PLAYER, foreign);
        assertThat(journal.detail(recordId).sourceId()).isEqualTo(middle);
        delete(type, PLAYER, middle);
        delete(type, PLAYER, middle);

        assertThat(countSource(type, middle)).isZero();
        assertThat(countSource(type, foreign)).isEqualTo(1);
        assertThat(countSource(sibling, siblingId)).isEqualTo(1);
        assertThat(count("outbox_events")).isEqualTo(outboxCount);
        LifeLogJournalResult.Page first = journal.list(null, null, 0, 2);
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.content()).extracting(LifeLogJournalResult.Entry::lifeLogId)
                .containsExactly(recordId(sibling, siblingId), recordId(type, newest));
        assertThat(journal.list(null, null, 1, 2).content())
                .extracting(LifeLogJournalResult.Entry::sourceId).containsExactly(oldest);
        assertThat(journal.list(roleId, null, 0, 20).totalElements()).isEqualTo(3);
        assertError(() -> journal.detail(recordId), LifeLogError.LIFE_LOG_NOT_FOUND);
        var summary = home.home();
        assertThat(summary.recentJournal()).hasSize(3);
        assertThat(summary.roleActivity30d().totalRecords()).isEqualTo(3);
        assertThat(summary.roleActivity30d().assignedRecords()).isEqualTo(3);
        assertThat(summary.roleActivity30d().roles()).singleElement()
                .satisfies(role -> assertThat(role.recordCount()).isEqualTo(3));
    }

    @ParameterizedTest
    @EnumSource(LifeLogSourceType.class)
    @DisplayName("Journal 삭제가 실패하면 이미 flush된 원본과 태그 삭제까지 rollback한다")
    void rollsBackSourceDeletion(LifeLogSourceType type) {
        Long sourceId = create(type, PLAYER);
        Long recordId = recordId(type, sourceId);
        int outboxCount = count("outbox_events");
        doAnswer(invocation -> {
            invocation.callRealMethod();
            entityManager.flush();
            assertThat(countSource(type, sourceId)).isZero();
            assertThat(count("life_log_records")).isZero();
            throw new IllegalStateException("forced Journal deletion failure");
        }).when(records).deleteBySourceAndPlayerId(type, sourceId, PLAYER);

        assertThatThrownBy(() -> delete(type, PLAYER, sourceId))
                .hasRootCauseInstanceOf(IllegalStateException.class);

        assertThat(countSource(type, sourceId)).isEqualTo(1);
        assertThat(journal.detail(recordId).sourceId()).isEqualTo(sourceId);
        if (type != LifeLogSourceType.EXERCISE) {
            assertThat(count(type == LifeLogSourceType.COLLECTION
                    ? "collection_log_tags" : "media_log_tags")).isEqualTo(1);
        }
        assertThat(home.home().recentJournal()).hasSize(1);
        assertThat(count("outbox_events")).isEqualTo(outboxCount);
    }

    @ParameterizedTest
    @EnumSource(LifeLogSourceType.class)
    @DisplayName("예기치 않은 원본 누락은 DELETE 재시도로 숨기지 않고 기존 invariant 오류를 유지한다")
    void retainsUnexpectedMissingSourceError(LifeLogSourceType type) {
        Long sourceId = create(type, PLAYER);
        Long recordId = recordId(type, sourceId);
        // Corruption fixture in this disposable test DB, not a supported deletion path.
        if (type != LifeLogSourceType.EXERCISE) {
            jdbc.update("DELETE FROM " + (type == LifeLogSourceType.COLLECTION
                    ? "collection_log_tags" : "media_log_tags"));
        }
        jdbc.update("DELETE FROM " + table(type) + " WHERE id = ?", sourceId);
        delete(type, PLAYER, sourceId);

        assertThat(count("life_log_records")).isEqualTo(1);
        assertError(() -> journal.detail(recordId), LifeLogError.LIFE_LOG_SOURCE_UNAVAILABLE);
        assertError(() -> journal.list(null, null, 0, 20), LifeLogError.LIFE_LOG_SOURCE_UNAVAILABLE);
        assertError(() -> home.home(), LifeLogError.LIFE_LOG_SOURCE_UNAVAILABLE);
    }

    private Long create(LifeLogSourceType type, Long playerId) {
        var metadata = new LifeLogRecordMetadataCommand(null, null,
                playerId.equals(PLAYER) ? roleId : null, null);
        return switch (type) {
            case COLLECTION -> collections.create(playerId, new CollectionCommand.Create(
                    "BOOK", "기록", null, 1, null, null, Set.of("tag"), metadata)).id();
            case MEDIA -> media.create(playerId, new MediaLogCommand.Create(
                    "MOVIE", "기록", null, 0, 1, "PLANNED", Set.of("tag"), metadata)).id();
            case EXERCISE -> exercises.create(playerId, new ExerciseCommand.Create(
                    "RUNNING", 30, null, null, LocalDate.now(), "기록", metadata)).id();
        };
    }

    private void delete(LifeLogSourceType type, Long playerId, Long id) {
        switch (type) {
            case COLLECTION -> collections.delete(playerId, id);
            case MEDIA -> media.delete(playerId, id);
            case EXERCISE -> exercises.delete(playerId, id);
        }
    }

    private Long recordId(LifeLogSourceType type, Long id) {
        return jdbc.queryForObject("SELECT id FROM life_log_records WHERE source_type = ? AND source_id = ?",
                Long.class, type.name(), id);
    }

    private int countSource(LifeLogSourceType type, Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table(type) + " WHERE id = ?", Integer.class, id);
    }

    private String table(LifeLogSourceType type) {
        return type.name().toLowerCase(java.util.Locale.ROOT) + "_logs";
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void assertError(Runnable action, LifeLogError error) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(error));
    }
}
