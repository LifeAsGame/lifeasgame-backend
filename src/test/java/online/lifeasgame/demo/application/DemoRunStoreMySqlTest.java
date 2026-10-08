package online.lifeasgame.demo.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.demo.domain.DemoError;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("포트폴리오 체험 run 저장소")
class DemoRunStoreMySqlTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_demo_runs")
            .withUsername("lifeasgame").withPassword("lifeasgame");

    private static DemoRunStore store;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        DataSource source = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Flyway.configure().dataSource(source).load().migrate();
        jdbc = new JdbcTemplate(source);
        store = new DemoRunStore(jdbc, new TransactionTemplate(new DataSourceTransactionManager(source)),
                new DemoProperties(true, 24, 3, 3, 3));
    }

    @Test
    @Order(1)
    @DisplayName("동시 재시도는 하나의 run만 만들고, 새 시도는 독립 run이며 총 상한을 지킨다")
    void concurrentStartAndCapacity() throws Exception {
        var proof = store.issueProof("cookie-a", "header-a");
        String key = "attempt-key-00000001";
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> store.start(proof.id(), key, "manager-a"));
            var second = pool.submit(() -> store.start(proof.id(), key, "manager-a"));
            assertThat(first.get().id()).isEqualTo(second.get().id());
        }
        var secondRun = store.start(proof.id(), "attempt-key-00000002", "manager-b");
        assertThat(secondRun.id()).isNotEqualTo(store.byManager("manager-a").id());
        store.start(proof.id(), "attempt-key-00000003", "manager-c");
        assertThatThrownBy(() -> store.start(proof.id(), "attempt-key-00000004", "manager-d"))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(DemoError.CAPACITY_EXCEEDED));
    }

    @Test
    @Order(3)
    @DisplayName("한 번 쓴 상대 연결 코드는 재사용할 수 없고 만료 run은 접근을 거부한다")
    void peerCodeAndExpiry() {
        String runId = store.byManager("manager-a").id();
        store.markReady(runId);
        store.issuePeerLink(runId, "one-time-code");
        assertThat(store.redeemPeerLink("one-time-code")).isEqualTo(runId);
        assertThatThrownBy(() -> store.redeemPeerLink("one-time-code"))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(DemoError.SESSION_INVALID));
        jdbc.update("UPDATE portfolio_demo_runs SET expires_at=? WHERE id=?",
                LocalDateTime.of(2000, 1, 1, 0, 0), runId);
        assertThat(store.byManager("manager-a").status()).isEqualTo("EXPIRED");
    }

    @Test
    @Order(2)
    @DisplayName("두 run과 일반 Player 사이에는 공유 경계가 없다")
    void actorBoundary() {
        String first = store.byManager("manager-a").id();
        String second = store.byManager("manager-b").id();
        store.bindActor(first, "explorer", 900001L, 800001L);
        store.bindActor(first, "seller", 900002L, 800002L);
        store.bindActor(second, "explorer", 900003L, 800003L);
        assertThat(store.sameBoundary(800001L, 800002L)).isTrue();
        assertThat(store.sameBoundary(800001L, 800003L)).isFalse();
        assertThat(store.sameBoundary(800001L, 12L)).isFalse();
        assertThat(store.sameBoundary(12L, 800001L)).isFalse();
        assertThat(store.sameBoundary(12L, 13L)).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("준비 실패는 같은 run에서 한 작업자만 다시 시작한다")
    void failedRunRetry() {
        String runId = store.byManager("manager-b").id();
        store.markFailed(runId);
        assertThat(store.byId(runId).status()).isEqualTo("FAILED");
        assertThat(store.retry(runId)).isTrue();
        assertThat(store.retry(runId)).isFalse();
        assertThat(store.claimProvisioning(runId)).isTrue();
        assertThat(store.claimProvisioning(runId)).isFalse();
        store.markReady(runId);
        assertThat(store.byId(runId).status()).isEqualTo("READY");
    }
}
