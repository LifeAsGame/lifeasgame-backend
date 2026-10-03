package online.lifeasgame.quest.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.CollectionLogService;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.role.application.RoleService;
import online.lifeasgame.role.application.command.RoleCommand;
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

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("백엔드 개발자 여정을 MySQL에서 진행할 때")
class BackendJourneyIntegrationTest {
    private static final long PLAYER = 980001L;
    private static final long OTHER = 980002L;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_backend_journey")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private RoleService roles;
    @Autowired private CollectionLogService collections;
    @Autowired private QuestRouteQueryService routes;
    @Autowired private QuestRouteSelectService select;
    @Autowired private QuestRouteAdvanceService advance;
    @Autowired private QuestService quests;
    @Autowired private BackendJourneyEvidenceService evidence;
    @MockitoBean private CurrentPlayerAccessor currentPlayer;
    @MockitoBean private PlayerTimezoneResolver timezone;
    @MockitoBean private DomainEventPublisher events;

    @Test
    @DisplayName("원본 7단계를 명시적 연결·완료·진행하며 소유권과 재전송을 지킨다")
    void progressesSevenSteps() {
        when(currentPlayer.currentPlayerIdOrThrow()).thenReturn(PLAYER);
        when(timezone.resolve(PLAYER)).thenReturn(ZoneId.of("Asia/Seoul"));
        long routeId = routes.routes().routes().stream()
                .filter(route -> route.code().equals(BackendJourneyAccess.ROUTE_CODE))
                .findFirst().orElseThrow().id();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quests WHERE code LIKE 'Q_DEV_%'", Integer.class))
                .isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_routes WHERE code='ROUTE_RECORD_START'", Integer.class))
                .isEqualTo(1);
        assertThatThrownBy(() -> select.select(routeId, null))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode").isEqualTo(QuestError.JOURNEY_ROLE_REQUIRED);

        long roleId = roles.create(new RoleCommand.Create(
                "ROLE_BACKEND_DEVELOPER", "Backend study", null)).id();
        long wrongType = roles.create(new RoleCommand.Create("PERSONAL", "Backend study", null)).id();
        assertThatThrownBy(() -> select.select(routeId, wrongType))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode").isEqualTo(QuestError.JOURNEY_ROLE_MISMATCH);
        var selected = select.select(routeId, roleId);
        assertThat(selected.playerProgress().roleId()).isEqualTo(roleId);
        assertThat(select.select(routeId, roleId).playerProgress().id())
                .isEqualTo(selected.playerProgress().id());
        assertThatThrownBy(() -> select.select(routeId, wrongType)).isInstanceOf(DomainException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_acceptances WHERE player_id=?", Integer.class, PLAYER))
                .isZero();

        List<QuestCode> codes = List.of(
                QuestCode.Q_DEV_DEFINE_BACKEND_GOAL,
                QuestCode.Q_DEV_RECORD_JAVA_STUDY,
                QuestCode.Q_DEV_BUILD_SPRING_CRUD,
                QuestCode.Q_DEV_MODEL_DATABASE,
                QuestCode.Q_DEV_WRITE_DOMAIN_TEST,
                QuestCode.Q_DEV_DEPLOY_SERVICE,
                QuestCode.Q_DEV_POLISH_README);
        long javaLog = record(PLAYER, "BOOK", "Java generics study", null);
        long projectLog = record(PLAYER, "PROJECT", "Spring service with tests", roleId);
        long foreignLog = record(OTHER, "PROJECT", "Other player's project", null);
        long wrongRoleLog = record(PLAYER, "PROJECT", "Different role project", wrongType);
        for (int index = 0; index < codes.size(); index++) {
            String code = codes.get(index).name();
            var accepted = quests.accept(new QuestCommand.Accept(code, null, null));
            assertThat(accepted.status()).isEqualTo("IN_PROGRESS");
            assertThatThrownBy(() -> quests.accept(new QuestCommand.Accept(code, null, null)))
                    .isInstanceOf(DomainException.class);
            if (index == 0) {
                evidence.linkMemo(code, "Build one backend service");
                assertThat(evidence.unlink(code).status()).isEqualTo("IN_PROGRESS");
                evidence.linkMemo(code, "Build one backend service");
            } else if (index == 1) {
                evidence.linkLifeLog(code, javaLog);
            } else if (index == 5) {
                assertThatThrownBy(() -> evidence.linkDeployment(code, "https://user:secret@example.org", "deployed"))
                        .isInstanceOf(DomainException.class);
                evidence.linkDeployment(code, "https://example.org/service", "Deployed service");
            } else {
                assertThatThrownBy(() -> evidence.linkLifeLog(code, foreignLog))
                        .isInstanceOf(DomainException.class);
                assertThatThrownBy(() -> evidence.linkLifeLog(code, wrongRoleLog))
                        .isInstanceOf(DomainException.class);
                evidence.linkLifeLog(code, projectLog);
            }
            assertThat(evidence.evidence(code)).isNotNull();
            assertThat(evidence.complete(code).status()).isEqualTo("COMPLETED");
            assertThat(evidence.complete(code).status()).isEqualTo("COMPLETED");
            assertThatThrownBy(() -> evidence.unlink(code)).isInstanceOf(DomainException.class);
            long stepId = routes.myRoute(routeId).playerProgress().currentStepId();
            if (index == 1) {
                collections.delete(PLAYER, jdbc.queryForObject(
                        "SELECT source_id FROM life_log_records WHERE id=?", Long.class, javaLog));
                assertThat(evidence.linkLifeLog(code, javaLog).status()).isEqualTo("COMPLETED");
            }
            advance.advance(routeId, stepId);
            if (index < 6) {
                assertThatThrownBy(() -> advance.advance(routeId, stepId))
                        .isInstanceOf(DomainException.class);
            }
        }
        assertThat(routes.myRoute(routeId).playerProgress().status()).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_journey_evidence WHERE acceptance_id IN (SELECT id FROM quest_acceptances WHERE player_id=?)", Integer.class, PLAYER))
                .isEqualTo(7);
    }

    private long record(long player, String category, String title, Long roleId) {
        return collections.create(player, new CollectionCommand.Create(
                category, title, null, 1, null, null, Set.of(),
                new LifeLogRecordMetadataCommand(
                        "PROJECT".equals(category) ? "PROJECT" : "STUDY", null, roleId, null)))
                .lifeLogId();
    }
}
