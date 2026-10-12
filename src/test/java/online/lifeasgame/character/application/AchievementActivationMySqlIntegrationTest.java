package online.lifeasgame.character.application;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.home.application.HomeQueryService;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.platform.outbox.OutboxProperties;
import online.lifeasgame.platform.outbox.application.OutboxRelayService;
import org.junit.jupiter.api.DisplayName;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("업적·칭호 자동 획득 MySQL 경계")
class AchievementActivationMySqlIntegrationTest {
    private static final AtomicLong USER_SEQUENCE = new AtomicLong(900_000);
    private static final Instant OCCURRED = Instant.parse("2026-10-06T00:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_achievement_activation")
            .withUsername("lifeasgame").withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired private PlayerRepository players;
    @Autowired private DomainEventPublisher publisher;
    @Autowired private OutboxRelayService relay;
    @Autowired private OutboxProperties outboxProperties;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private AchievementReconciliation reconciliation;
    @Autowired private PlayerAchievementService achievements;
    @Autowired private PlayerTitleService titles;
    @Autowired private ActivatedContentQuery contentQuery;
    @Autowired private HomeQueryService homeQueryService;
    @MockitoBean private CurrentPlayerAccessor currentPlayerAccessor;

    @Test
    @DisplayName("활성 목록은 현재 Player의 획득 상태만 안정된 페이지로 보여준다")
    void currentPlayerView() {
        Long first = player();
        Long second = player();
        transaction(() -> publisher.publish(fact(first, "owned", 401L)));
        configureRelay();
        relay.relayBatch();
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(second);

        var page = contentQuery.list(0, 7);
        assertThat(page.entries()).hasSize(7);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.entries()).allSatisfy(entry ->
                assertThat(entry.status()).isEqualTo("UNACQUIRED"));
        assertThat(contentQuery.list(1, 7).entries()).isEmpty();
    }

    @Test
    @DisplayName("같은 지급 시각을 활성 목록·보유 업적·보유 칭호·Home에서 동일하게 읽는다")
    void acquiredTimeMatchesAcrossReads() {
        Long playerId = player();
        transaction(() -> publisher.publish(fact(playerId, "time-contract", 501L)));
        configureRelay();
        relay.relayBatch();
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(playerId);

        var entries = contentQuery.list(0, 7).entries();
        var achievement = entries.stream()
                .filter(entry -> entry.code().equals("ACH_FIRST_LIFELOG"))
                .findFirst().orElseThrow();
        var title = entries.stream()
                .filter(entry -> entry.code().equals("TITLE_CANDIDATE_RECORD_BEGINNER"))
                .findFirst().orElseThrow();

        assertThat(achievement.sourceOccurredAt()).isEqualTo(OCCURRED);
        assertThat(achievements.getPlayerAchievementInfos()).singleElement()
                .extracting(info -> info.acquiredAt()).isEqualTo(achievement.acquiredAt());
        assertThat(titles.getPlayerTitleInfos()).singleElement()
                .extracting(info -> info.acquiredAt()).isEqualTo(title.acquiredAt());
        assertThat(homeQueryService.home().recentAchievements()).singleElement()
                .extracting(recent -> recent.acquiredAt()).isEqualTo(achievement.acquiredAt());

        Long legacyPlayer = player();
        var manualAchievement = achievements.grantAchievement(legacyPlayer,
                definitionId("achievements", "ACH_FIRST_LIFELOG"));
        var manualTitle = titles.createTitle(legacyPlayer,
                definitionId("titles", "TITLE_CANDIDATE_RECORD_BEGINNER"));
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(legacyPlayer);
        var manualEntries = contentQuery.list(0, 7).entries();
        assertThat(manualEntries).filteredOn(entry -> entry.code().equals("ACH_FIRST_LIFELOG"))
                .singleElement().satisfies(entry -> {
                    assertThat(entry.evidenceStatus()).isEqualTo("ADMIN_OR_LEGACY");
                    assertThat(entry.acquiredAt()).isEqualTo(manualAchievement.acquiredAt());
                });
        assertThat(manualEntries).filteredOn(entry -> entry.code().equals("TITLE_CANDIDATE_RECORD_BEGINNER"))
                .singleElement().satisfies(entry -> {
                    assertThat(entry.evidenceStatus()).isEqualTo("ADMIN_OR_LEGACY");
                    assertThat(entry.acquiredAt()).isEqualTo(manualTitle.acquiredAt());
                });
        assertThat(achievements.getPlayerAchievementInfos()).singleElement()
                .extracting(info -> info.acquiredAt()).isEqualTo(manualAchievement.acquiredAt());
        assertThat(titles.getPlayerTitleInfos()).singleElement()
                .extracting(info -> info.acquiredAt()).isEqualTo(manualTitle.acquiredAt());
    }

    @Test
    @DisplayName("두 원본과 재전달도 Player별 업적·연결 칭호 한 번으로 수렴한다")
    void duplicateFactsConverge() {
        Long playerId = player();
        transaction(() -> {
            publisher.publish(fact(playerId, "fact-1", 101L));
            publisher.publish(fact(playerId, "fact-2", 102L));
        });
        configureRelay();
        relay.relayBatch();
        relay.relayBatch();

        assertThat(count("player_achievements", playerId)).isEqualTo(1);
        assertThat(count("player_titles", playerId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM achievement_award_receipts
                WHERE player_id = ? AND achievement_code = 'ACH_FIRST_LIFELOG'
                AND status = 'GRANTED' AND provenance = 'DURABLE_EVENT'
                """, Integer.class, playerId)).isEqualTo(1);
    }

    @Test
    @DisplayName("원본 rollback은 fact를 남기지 않고 소급 dry-run·apply·재실행은 보존된 근거만 처리한다")
    void rollbackAndReconciliation() {
        Long playerId = player();
        assertThatThrownBy(() -> transaction(() -> {
            publisher.publish(fact(playerId, "rolled-back", 201L));
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(count("player_achievements", playerId)).isZero();

        transaction(() -> publisher.publish(fact(playerId, "committed", 202L)));
        var dryRun = reconciliation.run(playerId - 1, 1, false);
        assertThat(dryRun.players()).hasSize(1);
        assertThat(dryRun.players().getFirst().grantable()).isEqualTo(1);
        assertThat(count("player_achievements", playerId)).isZero();

        var applied = reconciliation.run(playerId - 1, 1, true);
        assertThat(applied.players().getFirst().grantable()).isEqualTo(1);
        assertThat(reconciliation.run(playerId - 1, 1, true).players()
                .getFirst().existing()).isGreaterThanOrEqualTo(1);
        configureRelay();
        relay.relayBatch();
        assertThat(count("player_achievements", playerId)).isEqualTo(1);
        assertThat(count("player_titles", playerId)).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자 회수 뒤 같은 사실 재전달은 보유를 복구하지 않는다")
    void revocationSurvivesReplay() {
        Long playerId = player();
        transaction(() -> publisher.publish(fact(playerId, "first", 301L)));
        configureRelay();
        relay.relayBatch();
        Long achievementId = definitionId("achievements", "ACH_FIRST_LIFELOG");
        Long titleId = definitionId("titles", "TITLE_CANDIDATE_RECORD_BEGINNER");
        achievements.revokeAchievement(playerId, achievementId);
        titles.revokeTitle(playerId, titleId);
        transaction(() -> publisher.publish(fact(playerId, "replay", 301L)));
        relay.relayBatch();
        assertThat(count("player_achievements", playerId)).isZero();
        assertThat(count("player_titles", playerId)).isZero();
    }

    private Long player() {
        return new TransactionTemplate(transactionManager).execute(status -> players.save(
                Player.linkStart(USER_SEQUENCE.incrementAndGet(), Name.of("Tester"),
                        GenderType.MALE)).getId());
    }

    private LifeLogRecorded fact(Long playerId, String eventId, Long lifeLogId) {
        return new LifeLogRecorded(eventId, LifeLogRecorded.EVENT_TYPE, 1, OCCURRED,
                playerId, lifeLogId, 1, LifeLogSubtype.QUICK_NOTE,
                LifeLogEntryMode.QUICK, null, null, null, null);
    }

    private void transaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private void configureRelay() {
        outboxProperties.setBatchSize(50);
        outboxProperties.setMaxAttempts(3);
        outboxProperties.setRetryDelayMs(0);
        outboxProperties.setInstanceId("achievement-activation-test");
    }

    private int count(String table, Long playerId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE player_id = ?",
                Integer.class, playerId);
    }

    private Long definitionId(String table, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE code = ?",
                Long.class, code);
    }
}
