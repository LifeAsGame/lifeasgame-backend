package online.lifeasgame.lifelog.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.query.CollectionQuery;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.domain.LifeLogCategoryKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("LifeLog 개인 분류 MySQL 계약")
class LifeLogCategoryIntegrationTest {
    private static final Long PLAYER = 88201L;
    private static final Long OTHER = 88202L;

    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired LifeLogCategoryService categories;
    @Autowired CollectionLogService collections;
    @Autowired CollectionLogQueryService collectionQuery;
    @Autowired LifeLogJournalQueryService journal;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean CurrentPlayerAccessor currentPlayer;

    @Test
    @DisplayName("동시 공용 추가는 같은 분류 ID로 수렴한다")
    void concurrentSystemAddConverges() throws Exception {
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(PLAYER);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return categories.addSystem("MEDIA", "ANIME").id(); });
            var second = executor.submit(() -> { start.await(); return categories.addSystem("MEDIA", "ANIME").id(); });
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(second.get(15, TimeUnit.SECONDS));
            assertThat(categories.mine("MEDIA")).hasSize(1);
        }
    }

    @Test
    @DisplayName("개인 분류 이름이 프로젝트여도 원본 OTHER는 PROJECT 근거로 바뀌지 않는다")
    void personalNameDoesNotChangeOriginalCategory() {
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(88203L);
        Long folder = categories.createPersonal("COLLECTION", "PROJECT").id();
        var created = collections.create(88203L, new CollectionCommand.Create(
                "OTHER", "Not project evidence", null, 1, null, null, Set.of(),
                LifeLogRecordMetadataCommand.none(), folder));
        assertThat(collectionQuery.getCollection(88203L, created.id()).category()).isEqualTo("OTHER");
        assertThat(collectionQuery.getCollection(88203L, created.id()).personalCategoryId()).isEqualTo(folder);
    }

    @Test
    @DisplayName("공용 선택·숨김 재시도, 개인 중복, 기록 배정·해제·삭제가 원본과 이벤트를 보존한다")
    void categoriesAndAssignmentsPreserveRecords() {
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(PLAYER);
        assertThat(categories.mine("COLLECTION")).isEmpty();
        assertThat(categories.systemCodes("COLLECTION")).contains("PROJECT");
        assertThat(categories.systemCodes("MEDIA")).doesNotContain("OTHER");

        Long systemId = categories.addSystem("COLLECTION", "PROJECT").id();
        assertThat(categories.addSystem("COLLECTION", "PROJECT").id()).isEqualTo(systemId);
        categories.hideSystem("COLLECTION", "PROJECT");
        assertThat(categories.mine("COLLECTION")).isEmpty();
        assertThat(categories.addSystem("COLLECTION", "PROJECT").id()).isEqualTo(systemId);

        Long personalId = categories.createPersonal("COLLECTION", "  Project notes  ").id();
        assertThat(categories.mine("COLLECTION")).extracting(LifeLogCategoryService.Category::name)
                .contains("PROJECT", "Project notes");
        assertThatThrownBy(() -> categories.createPersonal("COLLECTION", "project NOTES"))
                .isInstanceOf(DomainException.class);
        categories.createPersonal("EXERCISE", "Project notes");

        var created = collections.create(PLAYER, new CollectionCommand.Create(
                "PROJECT", "Original evidence", null, 1, null, null, Set.of(),
                LifeLogRecordMetadataCommand.none(), personalId));
        assertThat(collectionQuery.getCollection(PLAYER, created.id()).personalCategoryId()).isEqualTo(personalId);
        assertThat(collectionQuery.search(PLAYER, new CollectionQuery.Search(
                "PROJECT", "evidence", 0, 20, personalId, false))).hasSize(1);
        assertThat(collectionQuery.search(PLAYER, new CollectionQuery.Search(
                null, null, 0, 20, null, true))).isEmpty();
        assertThat(journal.list(null, null, 0, 20, personalId, false).content()).hasSize(1);
        assertThat(journal.list(null, null, 0, 20).content()).hasSize(1);

        int before = jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class);
        assertThatThrownBy(() -> categories.assign("EXERCISE", created.id(), personalId))
                .isInstanceOf(DomainException.class);
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(OTHER);
        assertThatThrownBy(() -> categories.assign("COLLECTION", created.id(), personalId))
                .isInstanceOf(DomainException.class);
        given(currentPlayer.currentPlayerIdOrThrow()).willReturn(PLAYER);
        assertThat(categories.assign("COLLECTION", created.id(), null)).isNull();
        assertThat(journal.list(null, null, 0, 20, null, true).content()).hasSize(1);
        categories.assign("COLLECTION", created.id(), personalId);
        categories.deletePersonal(personalId);
        assertThat(collectionQuery.getCollection(PLAYER, created.id()).personalCategoryId()).isNull();
        assertThat(collectionQuery.getCollection(PLAYER, created.id()).category()).isEqualTo("PROJECT");
        assertThat(journal.detail(created.lifeLogId()).sourceId()).isEqualTo(created.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isEqualTo(before);
        assertThat(categories.mine("COLLECTION")).extracting(LifeLogCategoryService.Category::name)
                .containsExactly("PROJECT");
    }
}
