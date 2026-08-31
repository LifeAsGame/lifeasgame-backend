package online.lifeasgame.character.application;

import online.lifeasgame.adminaudit.domain.error.AdminAuditError;
import online.lifeasgame.character.application.command.AdminPlayerHolderGrantCommand;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;
import online.lifeasgame.character.domain.repository.AchievementRepository;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import online.lifeasgame.character.domain.repository.TitleRepository;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentUserAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Admin Player holder grant MySQL transaction contract")
class AdminPlayerHolderGrantIntegrationTest {

    private static final long ADMIN_ID = 9316L;
    private static final long PLAYER_USER_ID = 19316L;

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_admin_holder_grant")
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
    private AdminPlayerHolderGrantService grantService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private TitleRepository titleRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserAccessor currentUserAccessor;

    private Long playerId;
    private Long achievementA;
    private Long achievementB;
    private Long titleA;
    private Long titleB;
    private Long representativeTitle;

    @BeforeEach
    void setUp() {
        jdbc.execute("DROP TRIGGER IF EXISTS force_admin_holder_audit_failure");
        jdbc.update("DELETE FROM admin_audit_events");
        jdbc.update("DELETE FROM player_achievements");
        jdbc.update("DELETE FROM player_titles");
        jdbc.update("DELETE FROM player WHERE user_id = ?", PLAYER_USER_ID);
        jdbc.update("DELETE FROM achievements WHERE code LIKE 'ADMIN-316-%'");
        jdbc.update("DELETE FROM titles WHERE code LIKE 'ADMIN-316-%'");
        jdbc.update(
                "DELETE FROM users WHERE id IN (?, ?)",
                ADMIN_ID,
                PLAYER_USER_ID
        );
        insertUser(
                ADMIN_ID,
                "holder-admin@example.com",
                "holder-admin",
                "ADMIN"
        );
        insertUser(
                PLAYER_USER_ID,
                "holder-player@example.com",
                "holder-player",
                "USER"
        );
        playerId = playerRepository.save(Player.linkStart(
                PLAYER_USER_ID,
                Name.of("holder-target"),
                GenderType.MALE
        )).getId();
        achievementA = achievement("ADMIN-316-ACH-A", "Achievement A");
        achievementB = achievement("ADMIN-316-ACH-B", "Achievement B");
        titleA = title("ADMIN-316-TITLE-A", "Title A");
        titleB = title("ADMIN-316-TITLE-B", "Title B");
        representativeTitle = title(
                "ADMIN-316-TITLE-REP",
                "Representative Title"
        );
        jdbc.execute("""
                CREATE TRIGGER force_admin_holder_audit_failure
                BEFORE INSERT ON admin_audit_events
                FOR EACH ROW
                SET NEW.actor_user_id = IF(
                    NEW.correlation_id = 'force-holder-audit-failure',
                    999999999,
                    NEW.actor_user_id
                )
                """);
        given(currentUserAccessor.currentUserIdOrThrow()).willReturn(ADMIN_ID);
    }

    @Test
    @DisplayName("같은 action/key의 동시 grant는 holder와 success Audit을 하나만 commit한다")
    void commitsSameKeyExactlyOnce() throws Exception {
        List<Outcome> achievements = concurrently(
                () -> grantService.grantAchievement(achievement(
                        achievementA,
                        "shared-achievement-key",
                        "request-achievement-a"
                )),
                () -> grantService.grantAchievement(achievement(
                        achievementB,
                        "shared-achievement-key",
                        "request-achievement-b"
                ))
        );
        assertExactlyOneIdempotencyConflict(achievements);

        List<Outcome> titles = concurrently(
                () -> grantService.grantTitle(title(
                        titleA,
                        "shared-title-key",
                        "request-title-a"
                )),
                () -> grantService.grantTitle(title(
                        titleB,
                        "shared-title-key",
                        "request-title-b"
                ))
        );
        assertExactlyOneIdempotencyConflict(titles);

        assertThat(holderCount("player_achievements")).isEqualTo(1);
        assertThat(holderCount("player_titles")).isEqualTo(1);
        assertThat(auditCount("PLAYER_ACHIEVEMENT_GRANT")).isEqualTo(1);
        assertThat(auditCount("PLAYER_TITLE_GRANT")).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 holder/different key의 동시 grant는 DB unique로 관계와 Audit을 하나만 남긴다")
    void keepsSameHolderUniqueAcrossDifferentKeys() throws Exception {
        List<Outcome> achievements = concurrently(
                () -> grantService.grantAchievement(achievement(
                        achievementA,
                        "achievement-holder-a",
                        "request-achievement-a"
                )),
                () -> grantService.grantAchievement(achievement(
                        achievementA,
                        "achievement-holder-b",
                        "request-achievement-b"
                ))
        );
        assertExactlyOneHolderConflict(achievements, "uq_player_achv");

        List<Outcome> titles = concurrently(
                () -> grantService.grantTitle(title(
                        titleA,
                        "title-holder-a",
                        "request-title-a"
                )),
                () -> grantService.grantTitle(title(
                        titleA,
                        "title-holder-b",
                        "request-title-b"
                ))
        );
        assertExactlyOneHolderConflict(titles, "uq_player_title");

        assertThat(holderCount("player_achievements")).isEqualTo(1);
        assertThat(holderCount("player_titles")).isEqualTo(1);
        assertThat(auditCount("PLAYER_ACHIEVEMENT_GRANT")).isEqualTo(1);
        assertThat(auditCount("PLAYER_TITLE_GRANT")).isEqualTo(1);

        assertHolderConflict(
                () -> grantService.grantAchievement(achievement(
                        achievementA,
                        "achievement-holder-c",
                        "request-achievement-c"
                )),
                "uq_player_achv"
        );
        assertHolderConflict(
                () -> grantService.grantTitle(title(
                        titleA,
                        "title-holder-c",
                        "request-title-c"
                )),
                "uq_player_title"
        );
        assertThat(auditCount("PLAYER_ACHIEVEMENT_GRANT")).isEqualTo(1);
        assertThat(auditCount("PLAYER_TITLE_GRANT")).isEqualTo(1);
    }

    @Test
    @DisplayName("Audit failure는 두 holder를 rollback하고 같은 key retry를 허용한다")
    void rollsBackAuditFailureWithoutConsumingKey() {
        assertThatThrownBy(() -> grantService.grantAchievement(achievement(
                achievementA,
                "achievement-audit-failure",
                "force-holder-audit-failure"
        ))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> grantService.grantTitle(title(
                titleA,
                "title-audit-failure",
                "force-holder-audit-failure"
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(holderCount("player_achievements")).isZero();
        assertThat(holderCount("player_titles")).isZero();
        assertThat(totalAuditCount()).isZero();

        grantService.grantAchievement(achievement(
                achievementA,
                "achievement-audit-failure",
                "request-achievement-retry"
        ));
        grantService.grantTitle(title(
                titleA,
                "title-audit-failure",
                "request-title-retry"
        ));

        assertThat(holderCount("player_achievements")).isEqualTo(1);
        assertThat(holderCount("player_titles")).isEqualTo(1);
        assertThat(totalAuditCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Title grant는 기존 representativeTitleId를 변경하지 않는다")
    void preservesRepresentativeTitle() {
        jdbc.update("""
                INSERT INTO player_titles (
                    player_id, title_id, acquired_at, created_at, updated_at
                ) VALUES (?, ?, NOW(6), NOW(6), NOW(6))
                """, playerId, representativeTitle);
        jdbc.update(
                "UPDATE player SET title_id = ? WHERE id = ?",
                representativeTitle,
                playerId
        );

        grantService.grantTitle(title(
                titleA,
                "title-representative",
                "request-title-representative"
        ));

        assertThat(representativeTitleId()).isEqualTo(representativeTitle);
        assertThat(holderCount("player_titles")).isEqualTo(2);
        assertThat(auditTarget("PLAYER_TITLE_GRANT"))
                .isEqualTo(playerId + ":" + titleA);
    }

    private void insertUser(
            long id,
            String email,
            String nickname,
            String authority
    ) {
        jdbc.update("""
                INSERT INTO users (
                    id, email, password_hash, nickname, status,
                    account_authority, created_at, updated_at
                ) VALUES (?, ?, 'hash', ?, 'ACTIVE', ?, NOW(6), NOW(6))
                """, id, email, nickname, authority);
    }

    private Long achievement(String code, String name) {
        return achievementRepository.save(Achievement.create(
                code,
                name,
                AchievementCategory.STORY,
                null
        )).getId();
    }

    private Long title(String code, String name) {
        return titleRepository.save(Title.create(
                code,
                name,
                TitleCategory.SPECIAL,
                null
        )).getId();
    }

    private AdminPlayerHolderGrantCommand.GrantAchievement achievement(
            Long achievementId,
            String key,
            String correlationId
    ) {
        return new AdminPlayerHolderGrantCommand.GrantAchievement(
                playerId,
                achievementId,
                "CASE-316-ACHIEVEMENT",
                key,
                correlationId
        );
    }

    private AdminPlayerHolderGrantCommand.GrantTitle title(
            Long titleId,
            String key,
            String correlationId
    ) {
        return new AdminPlayerHolderGrantCommand.GrantTitle(
                playerId,
                titleId,
                "CASE-316-TITLE",
                key,
                correlationId
        );
    }

    private List<Outcome> concurrently(Runnable first, Runnable second)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstResult = executor.submit(
                    () -> invoke(ready, start, first)
            );
            Future<Outcome> secondResult = executor.submit(
                    () -> invoke(ready, start, second)
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Outcome invoke(
            CountDownLatch ready,
            CountDownLatch start,
            Runnable command
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Outcome(false, new IllegalStateException("start timeout"));
            }
            command.run();
            return new Outcome(true, null);
        } catch (Throwable exception) {
            return new Outcome(false, exception);
        }
    }

    private void assertExactlyOneIdempotencyConflict(List<Outcome> outcomes) {
        assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.success())
                .singleElement()
                .satisfies(outcome -> {
                    assertThat(outcome.failure())
                            .isInstanceOf(DomainException.class);
                    assertThat(((DomainException) outcome.failure())
                            .getErrorCode())
                            .isEqualTo(
                                    AdminAuditError.DUPLICATE_IDEMPOTENCY_KEY
                            );
                });
    }

    private void assertExactlyOneHolderConflict(
            List<Outcome> outcomes,
            String constraint
    ) {
        assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.success())
                .singleElement()
                .satisfies(outcome -> assertHolderConflict(
                        outcome.failure(),
                        constraint
                ));
    }

    private void assertHolderConflict(Runnable command, String constraint) {
        assertThatThrownBy(command::run)
                .satisfies(failure -> assertHolderConflict(
                        failure,
                        constraint
                ));
    }

    private void assertHolderConflict(Throwable failure, String constraint) {
        assertThat(failure).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(allMessages(failure).toLowerCase()).contains(constraint);
    }

    private String allMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null;
             current = current.getCause()) {
            if (current.getMessage() != null) {
                messages.append(' ').append(current.getMessage());
            }
        }
        return messages.toString();
    }

    private int holderCount(String table) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE player_id = ?",
                Integer.class,
                playerId
        );
    }

    private int auditCount(String action) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM admin_audit_events
                WHERE action = ? AND result = 'SUCCESS'
                """, Integer.class, action);
    }

    private int totalAuditCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_events",
                Integer.class
        );
    }

    private String auditTarget(String action) {
        return jdbc.queryForObject(
                "SELECT target_id FROM admin_audit_events WHERE action = ?",
                String.class,
                action
        );
    }

    private Long representativeTitleId() {
        return jdbc.queryForObject(
                "SELECT title_id FROM player WHERE id = ?",
                Long.class,
                playerId
        );
    }

    private record Outcome(boolean success, Throwable failure) {
    }
}
