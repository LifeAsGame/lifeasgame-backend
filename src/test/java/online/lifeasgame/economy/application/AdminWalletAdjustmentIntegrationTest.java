package online.lifeasgame.economy.application;

import online.lifeasgame.adminaudit.domain.error.AdminAuditError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.economy.application.command.AdminWalletAdjustmentCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Admin Wallet adjustment MySQL transaction contract")
class AdminWalletAdjustmentIntegrationTest {

    private static final long ADMIN_ID = 9306L;
    private static final long PLAYER_ID = 19306L;

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_admin_wallet")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame")
                    .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired
    private AdminWalletAdjustmentService adjustmentService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserAccessor currentUserAccessor;

    @BeforeEach
    void setUp() {
        jdbc.execute("DROP TRIGGER IF EXISTS force_admin_audit_failure");
        jdbc.update("DELETE FROM admin_audit_events");
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM wallet_holds");
        jdbc.update("DELETE FROM wallet_balances");
        jdbc.update("DELETE FROM wallets");
        jdbc.update("DELETE FROM users WHERE id = ?", ADMIN_ID);
        jdbc.update("""
                INSERT INTO users (
                    id, email, password_hash, nickname, status,
                    account_authority, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'ACTIVE', 'ADMIN', NOW(6), NOW(6))
                """, ADMIN_ID, "wallet-admin@example.com", "hash", "wallet-admin");
        jdbc.execute("""
                CREATE TRIGGER force_admin_audit_failure
                BEFORE INSERT ON admin_audit_events
                FOR EACH ROW
                SET NEW.actor_user_id = IF(
                    NEW.correlation_id = 'force-audit-failure',
                    999999999,
                    NEW.actor_user_id
                )
                """);
        given(currentUserAccessor.currentUserIdOrThrow()).willReturn(ADMIN_ID);
    }

    @Nested
    @DisplayName("valid adjustment를 실행할 때")
    class Success {

        @Test
        @DisplayName("credit은 Wallet, outbox, canonical Audit을 함께 commit한다")
        void creditsAtomically() {
            EconomyResult.WalletBalance result = adjustmentService.adjust(
                    command(100, false, "credit-306", "request-credit")
            );

            assertThat(result.amount()).isEqualTo(100L);
            assertThat(balance()).isEqualTo(100L);
            assertThat(outboxCount()).isEqualTo(1);
            assertThat(auditCount()).isEqualTo(1);
            assertThat(auditRow()).containsExactly(
                    ADMIN_ID,
                    "WALLET_ADJUSTMENT",
                    "WALLET",
                    Long.toString(PLAYER_ID),
                    "CASE-306",
                    "SUCCESS",
                    "request-credit",
                    "credit-306"
            );
        }

        @Test
        @DisplayName("debit은 기존 pessimistic lock과 delta semantics를 유지한다")
        void debitsWithExistingLock() {
            seedWallet(200L);

            EconomyResult.WalletBalance result = adjustmentService.adjust(
                    command(70, true, "debit-306", "request-debit")
            );

            assertThat(result.amount()).isEqualTo(130L);
            assertThat(balance()).isEqualTo(130L);
            assertThat(auditCount()).isEqualTo(1);
            assertThat(outboxCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("required persistence가 실패할 때")
    class FailClosed {

        @Test
        @DisplayName("Audit/idempotency insert 실패는 Wallet과 outbox를 rollback한다")
        void rollsBackAuditFailure() {
            seedWallet(100L);

            assertThatThrownBy(() -> adjustmentService.adjust(command(
                    30,
                    false,
                    "audit-failure-306",
                    "force-audit-failure"
            ))).isInstanceOf(DataIntegrityViolationException.class);

            assertThat(balance()).isEqualTo(100L);
            assertThat(auditCount()).isZero();
            assertThat(outboxCount()).isZero();
        }

        @Test
        @DisplayName("debit business failure는 key를 소비하지 않아 retry할 수 있다")
        void retriesKeyAfterBusinessFailure() {
            seedWallet(20L);
            String key = "retry-after-debit-failure";

            assertThatThrownBy(() -> adjustmentService.adjust(command(
                    30,
                    true,
                    key,
                    "request-failed-debit"
            ))).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("insufficient balance");
            assertThat(balance()).isEqualTo(20L);
            assertThat(auditCount()).isZero();
            assertThat(outboxCount()).isZero();

            adjustmentService.adjust(command(
                    5,
                    false,
                    key,
                    "request-retry"
            ));

            assertThat(balance()).isEqualTo(25L);
            assertThat(auditCount()).isEqualTo(1);
            assertThat(outboxCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("같은 idempotency key를 재사용할 때")
    class Duplicates {

        @Test
        @DisplayName("sequential duplicate는 409 conflict이고 effect는 한 번이다")
        void rejectsSequentialDuplicate() {
            seedWallet(0L);
            String key = "sequential-duplicate-306";
            adjustmentService.adjust(command(10, false, key, "request-first"));

            assertThatThrownBy(() -> adjustmentService.adjust(command(
                    10,
                    false,
                    key,
                    "request-second"
            ))).isInstanceOfSatisfying(DomainException.class, exception ->
                    assertThat(exception.getErrorCode())
                            .isEqualTo(AdminAuditError.DUPLICATE_IDEMPOTENCY_KEY)
            );

            assertThat(balance()).isEqualTo(10L);
            assertThat(auditCount()).isEqualTo(1);
            assertThat(outboxCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("concurrent duplicate는 DB uniqueness와 Wallet lock으로 effect가 정확히 한 번이다")
        void rejectsConcurrentDuplicate() throws Exception {
            seedWallet(0L);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            try {
                Future<Outcome> first = executor.submit(() -> invoke(
                        ready,
                        start,
                        "concurrent-duplicate-306",
                        "request-concurrent-1"
                ));
                Future<Outcome> second = executor.submit(() -> invoke(
                        ready,
                        start,
                        "concurrent-duplicate-306",
                        "request-concurrent-2"
                ));
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                List<Outcome> outcomes = List.of(
                        first.get(10, TimeUnit.SECONDS),
                        second.get(10, TimeUnit.SECONDS)
                );
                assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
                assertThat(outcomes).filteredOn(outcome -> !outcome.success())
                        .singleElement()
                        .satisfies(outcome -> {
                            assertThat(outcome.failure())
                                    .isInstanceOf(DomainException.class);
                            DomainException exception =
                                    (DomainException) outcome.failure();
                            assertThat(exception.getErrorCode()).isEqualTo(
                                    AdminAuditError.DUPLICATE_IDEMPOTENCY_KEY
                            );
                        });
            } finally {
                executor.shutdownNow();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                        .isTrue();
            }

            assertThat(balance()).isEqualTo(10L);
            assertThat(auditCount()).isEqualTo(1);
            assertThat(outboxCount()).isEqualTo(1);
        }
    }

    private Outcome invoke(
            CountDownLatch ready,
            CountDownLatch start,
            String key,
            String correlationId
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Outcome(false, new IllegalStateException("start timeout"));
            }
            adjustmentService.adjust(command(10, false, key, correlationId));
            return new Outcome(true, null);
        } catch (Throwable exception) {
            return new Outcome(false, exception);
        }
    }

    private AdminWalletAdjustmentCommand command(
            long amount,
            boolean debit,
            String idempotencyKey,
            String correlationId
    ) {
        return new AdminWalletAdjustmentCommand(
                PLAYER_ID,
                amount,
                "GOLD",
                debit,
                "CASE-306",
                idempotencyKey,
                correlationId
        );
    }

    private void seedWallet(long amount) {
        jdbc.update("""
                INSERT INTO wallets (
                    owner_id, version, created_at, updated_at
                ) VALUES (?, 0, NOW(6), NOW(6))
                """, PLAYER_ID);
        Long walletId = jdbc.queryForObject(
                "SELECT id FROM wallets WHERE owner_id = ?",
                Long.class,
                PLAYER_ID
        );
        jdbc.update("""
                INSERT INTO wallet_balances (
                    wallet_id, currency, amount, created_at, updated_at
                ) VALUES (?, 'GOLD', ?, NOW(6), NOW(6))
                """, walletId, amount);
    }

    private long balance() {
        return jdbc.query("""
                        SELECT b.amount
                        FROM wallet_balances b
                        JOIN wallets w ON w.id = b.wallet_id
                        WHERE w.owner_id = ? AND b.currency = 'GOLD'
                        """, (result, row) -> result.getLong(1), PLAYER_ID)
                .stream()
                .findFirst()
                .orElse(0L);
    }

    private int auditCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_events",
                Integer.class
        );
    }

    private int outboxCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_events",
                Integer.class
        );
    }

    private List<Object> auditRow() {
        return jdbc.queryForObject("""
                SELECT actor_user_id, action, target_type, target_id,
                       reason, result, correlation_id, idempotency_key
                FROM admin_audit_events
                """, (result, row) -> List.of(
                result.getLong("actor_user_id"),
                result.getString("action"),
                result.getString("target_type"),
                result.getString("target_id"),
                result.getString("reason"),
                result.getString("result"),
                result.getString("correlation_id"),
                result.getString("idempotency_key")
        ));
    }

    private record Outcome(boolean success, Throwable failure) {
    }
}
