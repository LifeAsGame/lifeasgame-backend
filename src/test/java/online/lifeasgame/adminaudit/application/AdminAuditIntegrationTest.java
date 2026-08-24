package online.lifeasgame.adminaudit.application;

import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.application.result.AdminAuditQueryResult;
import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.flywaydb.core.Flyway;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Admin Audit MySQL persistence와 transaction contract")
class AdminAuditIntegrationTest {

    private static final long ACTOR_ID = 9304L;
    private static final long MISSING_ACTOR_ID = 9305L;

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_admin_audit")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
    }

    @Autowired
    private AdminAuditInternalApi auditApi;

    @Autowired
    private AdminAuditQueryService queryService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private CurrentUserAccessor currentUserAccessor;

    @MockitoBean
    private UserAuthApi userAuthApi;

    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS admin_audit_test_mutations (
                    id BIGINT NOT NULL PRIMARY KEY,
                    note VARCHAR(32) NOT NULL
                ) ENGINE=InnoDB
                """);
        jdbc.update("DELETE FROM admin_audit_events");
        jdbc.update("DELETE FROM admin_audit_test_mutations");
        jdbc.update("DELETE FROM users WHERE id IN (?, ?)", ACTOR_ID, MISSING_ACTOR_ID);
        jdbc.update("""
                INSERT INTO users (
                    id, email, password_hash, nickname, status,
                    account_authority, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'ACTIVE', 'ADMIN', NOW(6), NOW(6))
                """, ACTOR_ID, "audit-admin@example.com", "hash", "audit-admin");
        given(currentUserAccessor.currentUserIdOrThrow())
                .willReturn(ACTOR_ID);
        given(userAuthApi.resolveAuthorization(ACTOR_ID))
                .willReturn(Optional.of(
                        new UserAuthApi.AccountAuthorization(true, true)
                ));
        given(userAuthApi.resolveAuthorization(MISSING_ACTOR_ID))
                .willReturn(Optional.of(
                        new UserAuthApi.AccountAuthorization(true, true)
                ));
        transaction = new TransactionTemplate(transactionManager);
    }

    @Nested
    @DisplayName("caller transaction에서 append할 때")
    class TransactionContract {

        @Test
        @DisplayName("business mutation과 durable audit을 함께 commit한다")
        void commitsTogether() {
            transaction.executeWithoutResult(status -> {
                jdbc.update(
                        "INSERT INTO admin_audit_test_mutations (id, note) VALUES (1, 'changed')"
                );
                auditApi.append(command(
                        "USER_STATUS_CHANGE",
                        "USER",
                        "42",
                        "request-commit"
                ));
            });

            assertThat(flyway.info().current().getVersion().getVersion())
                    .isEqualTo("30");
            assertThat(count("admin_audit_test_mutations")).isEqualTo(1);
            assertThat(count("admin_audit_events")).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "SELECT actor_user_id FROM admin_audit_events",
                    Long.class
            )).isEqualTo(ACTOR_ID);
        }

        @Test
        @DisplayName("audit persistence 실패는 business mutation도 rollback한다")
        void failsClosed() {
            given(currentUserAccessor.currentUserIdOrThrow())
                    .willReturn(MISSING_ACTOR_ID);

            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                jdbc.update(
                        "INSERT INTO admin_audit_test_mutations (id, note) VALUES (2, 'changed')"
                );
                auditApi.append(command(
                        "USER_STATUS_CHANGE",
                        "USER",
                        "42",
                        "request-failure"
                ));
            })).isInstanceOf(DataIntegrityViolationException.class);

            assertThat(count("admin_audit_test_mutations")).isZero();
            assertThat(count("admin_audit_events")).isZero();
        }

        @Test
        @DisplayName("authenticated USER actor면 거부하고 business mutation도 rollback한다")
        void rejectsNonAdminAndRollsBack() {
            given(userAuthApi.resolveAuthorization(ACTOR_ID))
                    .willReturn(Optional.of(
                            new UserAuthApi.AccountAuthorization(true, false)
                    ));

            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                jdbc.update(
                        "INSERT INTO admin_audit_test_mutations (id, note) VALUES (3, 'changed')"
                );
                auditApi.append(command(
                        "USER_STATUS_CHANGE",
                        "USER",
                        "42",
                        "request-non-admin"
                ));
            })).isInstanceOfSatisfying(AuthException.class, exception ->
                    assertThat(exception.getErrorCode())
                            .isEqualTo(AuthError.FORBIDDEN)
            );

            assertThat(count("admin_audit_test_mutations")).isZero();
            assertThat(count("admin_audit_events")).isZero();
        }

        @Test
        @DisplayName("caller transaction이 없으면 append를 거부한다")
        void requiresCallerTransaction() {
            assertThatThrownBy(() -> auditApi.append(command(
                    "USER_STATUS_CHANGE",
                    "USER",
                    "42",
                    "request-without-transaction"
            ))).isInstanceOf(
                    org.springframework.transaction.IllegalTransactionStateException.class
            );
        }
    }

    @Nested
    @DisplayName("audit event를 조회할 때")
    class QueryContract {

        @Test
        @DisplayName("filters와 occurredAt-id cursor로 안정적인 bounded page를 반환한다")
        void filtersAndPaginates() {
            AdminAuditInternalApi.AppendResult first = append(command(
                    "USER_STATUS_CHANGE", "USER", "42", "request-1"
            ));
            AdminAuditInternalApi.AppendResult second = append(command(
                    "WALLET_ADJUSTMENT", "WALLET", "9", "request-2"
            ));
            AdminAuditInternalApi.AppendResult third = append(command(
                    "USER_STATUS_CHANGE", "USER", "43", "request-3"
            ));

            AdminAuditQueryResult.Page firstPage = queryService.list(
                    ACTOR_ID, null, null, null, null, null,
                    null, null, null, 2
            );
            AdminAuditQueryResult.Page secondPage = queryService.list(
                    ACTOR_ID, null, null, null, null, null,
                    null, null, firstPage.nextCursor(), 2
            );
            AdminAuditQueryResult.Page filtered = queryService.list(
                    ACTOR_ID,
                    "USER_STATUS_CHANGE",
                    "USER",
                    "42",
                    AdminAuditResult.SUCCESS,
                    "request-1",
                    first.occurredAt().minusSeconds(1),
                    first.occurredAt().plusSeconds(1),
                    null,
                    20
            );

            assertThat(firstPage.items())
                    .extracting(AdminAuditQueryResult.Item::id)
                    .containsExactly(third.auditEventId(), second.auditEventId());
            assertThat(firstPage.nextCursor()).isNotNull();
            assertThat(secondPage.items())
                    .extracting(AdminAuditQueryResult.Item::id)
                    .containsExactly(first.auditEventId());
            assertThat(secondPage.nextCursor()).isNull();
            assertThat(filtered.items()).singleElement().satisfies(item -> {
                assertThat(item.id()).isEqualTo(first.auditEventId());
                assertThat(item.reason()).isEqualTo("CASE-304");
                assertThat(item.idempotencyKey()).isNull();
            });
        }
    }

    private AdminAuditInternalApi.AppendResult append(
            AdminAuditInternalApi.AppendCommand command
    ) {
        return transaction.execute(status -> auditApi.append(command));
    }

    private AdminAuditInternalApi.AppendCommand command(
            String action,
            String targetType,
            String targetId,
            String correlationId
    ) {
        return new AdminAuditInternalApi.AppendCommand(
                new AdminAuditAction(action),
                new AdminAuditTargetType(targetType),
                targetId,
                "CASE-304",
                AdminAuditResult.SUCCESS,
                correlationId,
                null
        );
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
