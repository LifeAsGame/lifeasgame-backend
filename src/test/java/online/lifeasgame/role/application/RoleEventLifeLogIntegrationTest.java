package online.lifeasgame.role.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.role.application.command.RoleEventCommand;
import online.lifeasgame.role.application.result.RoleEventResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("RoleEvent와 explicit LifeLog linkage MySQL 흐름")
class RoleEventLifeLogIntegrationTest {

    private static final Long PLAYER_ID = 25201L;

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_role_event_lifelog")
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
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired
    private RoleEventService roleEventService;

    @Autowired
    private LifeLogRecordRegistrar registrar;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    @BeforeEach
    void cleanState() {
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willReturn(PLAYER_ID);
        jdbc.update(
                "DELETE FROM life_log_records WHERE player_id = ?",
                PLAYER_ID
        );
        jdbc.update("DELETE FROM role_event_participants");
        jdbc.update(
                "DELETE FROM role_events WHERE player_id = ?",
                PLAYER_ID
        );
        jdbc.update("DELETE FROM roles WHERE player_id = ?", PLAYER_ID);
    }

    @Nested
    @DisplayName("RoleEvent lifecycle을 실행하면")
    class NoAutomaticLifeLog {

        @Test
        @DisplayName("create와 complete 모두 LifeLogRecord를 자동 생성하지 않는다")
        void neverCreatesLifeLog() {
            Long roleId = insertRole();

            RoleEventResult.Detail event = roleEventService.create(
                    roleId,
                    new RoleEventCommand.Create(
                            "팀 회고",
                            null,
                            null,
                            null
                    )
            );
            roleEventService.complete(roleId, event.id());

            assertThat(lifeLogCount()).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT status FROM role_events WHERE id = ?",
                    String.class,
                    event.id()
            )).isEqualTo("COMPLETED");
        }
    }

    @Nested
    @DisplayName("LifeLog를 RoleEvent에 명시적으로 연결하면")
    class ExplicitLinkage {

        @Test
        @DisplayName("event의 Role을 derive해 canonical header에 원자적으로 저장한다")
        void persistsDerivedRoleContext() {
            Long roleId = insertRole();
            RoleEventResult.Detail event = roleEventService.create(
                    roleId,
                    new RoleEventCommand.Create(
                            "팀 회고",
                            null,
                            null,
                            null
                    )
            );

            LifeLogRecord record = new TransactionTemplate(transactionManager)
                    .execute(status -> registrar.register(
                            PLAYER_ID,
                            LifeLogSourceType.COLLECTION,
                            252001L,
                            LifeLogEntryMode.FULL,
                            new LifeLogRecordMetadataCommand(
                                    null,
                                    null,
                                    null,
                                    event.id()
                            )
                    ));

            Map<String, Object> stored = jdbc.queryForMap("""
                    SELECT primary_role_id, role_event_id
                    FROM life_log_records
                    WHERE id = ?
                    """, record.getId());
            assertThat(stored)
                    .containsEntry("primary_role_id", roleId)
                    .containsEntry("role_event_id", event.id());
        }
    }

    private Long insertRole() {
        jdbc.update("""
                INSERT INTO roles (
                    player_id, role_type, name, description, status,
                    created_at, updated_at, version
                ) VALUES (?, 'WORK', 'Developer', NULL, 'ACTIVE',
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 0)
                """, PLAYER_ID);
        return jdbc.queryForObject(
                "SELECT id FROM roles WHERE player_id = ?",
                Long.class,
                PLAYER_ID
        );
    }

    private long lifeLogCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM life_log_records WHERE player_id = ?",
                Long.class,
                PLAYER_ID
        );
    }
}
