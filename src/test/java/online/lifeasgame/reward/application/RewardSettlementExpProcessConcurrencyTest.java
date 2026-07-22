package online.lifeasgame.reward.application;

import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.event.PlayerLeveledUp;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.result.RewardSettlementExpProcessResult;
import online.lifeasgame.reward.application.result.RewardSettlementLineRetryPreparationResult;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import online.lifeasgame.reward.domain.RewardSettlementStatus;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import(RewardSettlementExpProcessConcurrencyTest.EventProbeConfig.class)
@DisplayName("RewardSettlement EXP 처리 MySQL 동시성")
class RewardSettlementExpProcessConcurrencyTest {

    private static final long PLAYER_ID = 187001L;
    private static final RewardSettlementSourceType SOURCE_TYPE =
            RewardSettlementSourceType.QUEST_COMPLETION;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_exp_reward_concurrency")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private RewardSettlementCreateService createService;

    @Autowired
    private RewardSettlementExpProcessService processService;

    @Autowired
    private RewardSettlementLineFailureRecorder failureRecorder;

    @Autowired
    private RewardSettlementLineRetryPreparationService retryPreparationService;

    @MockitoSpyBean
    private RewardSettlementWriter settlementWriter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LevelUpCommitProbe levelUpCommitProbe;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM player_growth_changes");
        jdbcTemplate.update("DELETE FROM reward_settlement_lines");
        jdbcTemplate.update("DELETE FROM reward_settlements");
        jdbcTemplate.update("DELETE FROM player WHERE id = ?", PLAYER_ID);
        levelUpCommitProbe.reset();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Nested
    @DisplayName("같은 EXP Line에 두 요청이 동시에 도착할 때")
    class ProcessSameLineConcurrently {

        @Test
        @DisplayName("Player EXP와 GrowthChange는 한 번만 반영되고 두 결과는 success/replay가 된다")
        void grantsExactlyOnce() throws Exception {
            insertPlayer(0L);
            RewardSettlement settlement = createSettlement(187101L, "RP_EXP_10");
            Long lineId = settlement.getLines().getFirst().getId();

            List<RewardSettlementExpProcessResult> results = runConcurrently(
                    () -> processService.process(settlement.getId(), lineId),
                    () -> processService.process(settlement.getId(), lineId)
            );

            assertThat(playerExp()).isEqualTo(10L);
            assertThat(growthChangeCount()).isEqualTo(1);
            assertThat(growthChangeCountByRewardLine(lineId)).isEqualTo(1);
            assertThat(lineStatus(lineId)).isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
            assertThat(settlementStatus(settlement.getId()))
                    .isEqualTo(RewardSettlementStatus.COMPLETED.name());
            assertThat(results).extracting(RewardSettlementExpProcessResult::replayed)
                    .containsExactlyInAnyOrder(false, true);
            assertThat(results).extracting(RewardSettlementExpProcessResult::growthChangeId)
                    .doesNotContainNull()
                    .containsOnly(results.getFirst().growthChangeId());
            assertThat(settlementRowCount()).isEqualTo(1);
            assertThat(settlementLineRowCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("서로 다른 EXP Line이 같은 Player를 동시에 변경할 때")
    class ProcessDifferentLinesForSamePlayer {

        @Test
        @DisplayName("Settlement 다음 Player 잠금 순서로 두 지급을 모두 반영하고 lost update가 없다")
        void preventsLostUpdate() throws Exception {
            insertPlayer(0L);
            RewardSettlement first = createSettlement(187201L, "RP_EXP_10");
            RewardSettlement second = createSettlement(187202L, "RP_EXP_30");
            Long firstLineId = first.getLines().getFirst().getId();
            Long secondLineId = second.getLines().getFirst().getId();

            List<RewardSettlementExpProcessResult> results = runConcurrently(
                    () -> processService.process(first.getId(), firstLineId),
                    () -> processService.process(second.getId(), secondLineId)
            );

            assertThat(playerExp()).isEqualTo(40L);
            assertThat(growthChangeCount()).isEqualTo(2);
            assertThat(results).extracting(RewardSettlementExpProcessResult::appliedExp)
                    .containsExactlyInAnyOrder(10L, 30L);
            assertThat(results).extracting(RewardSettlementExpProcessResult::replayed)
                    .containsOnly(false);
            assertThat(lineStatus(firstLineId)).isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
            assertThat(lineStatus(secondLineId)).isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
        }
    }

    @Nested
    @DisplayName("최초 성공 응답을 잃고 같은 command를 다시 실행할 때")
    class ReplayCommittedResponse {

        @Test
        @DisplayName("기존 GrowthChange를 반환하고 EXP와 Level Up event를 다시 적용하지 않는다")
        void replaysWithoutMutationOrDuplicateEvent() {
            insertPlayer(95L);
            RewardSettlement settlement = createSettlement(187301L, "RP_EXP_10");
            Long lineId = settlement.getLines().getFirst().getId();

            RewardSettlementExpProcessResult first =
                    processService.process(settlement.getId(), lineId);
            RewardSettlementExpProcessResult replay =
                    processService.process(settlement.getId(), lineId);

            assertThat(first.replayed()).isFalse();
            assertThat(replay.replayed()).isTrue();
            assertThat(replay.growthChangeId()).isEqualTo(first.growthChangeId());
            assertThat(playerExp()).isEqualTo(105L);
            assertThat(playerLevel()).isEqualTo(2);
            assertThat(growthChangeCount()).isEqualTo(1);
            assertThat(levelUpCommitProbe.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("성공 attempt 중 예상하지 못한 RuntimeException이 발생할 때")
    class RollBackUnexpectedFailure {

        @Test
        @DisplayName("Player와 GrowthChange를 rollback하고 Line PENDING 및 Level Up 미발행을 유지한다")
        void rollsBackAllAtomicChanges() {
            insertPlayer(95L);
            RewardSettlement settlement = createSettlement(187401L, "RP_EXP_10");
            Long lineId = settlement.getLines().getFirst().getId();
            doThrow(new RuntimeException("forced after growth flush"))
                    .when(settlementWriter)
                    .saveAndFlush(argThat(candidate -> settlement.getId().equals(candidate.getId())));

            assertThatThrownBy(() -> processService.process(settlement.getId(), lineId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("forced after growth flush");

            assertThat(playerExp()).isEqualTo(95L);
            assertThat(playerLevel()).isEqualTo(1);
            assertThat(growthChangeCount()).isZero();
            assertThat(lineStatus(lineId)).isEqualTo(RewardSettlementLineStatus.PENDING.name());
            assertThat(levelUpCommitProbe.count()).isZero();
        }
    }

    @Nested
    @DisplayName("알려진 DomainException으로 성공 attempt가 rollback될 때")
    class RecordKnownFailure {

        @Test
        @DisplayName("Player가 없으면 GrowthChange 없이 Line FAILED와 안정된 failureCode만 commit한다")
        void recordsPlayerNotFound() {
            RewardSettlement settlement = createSettlement(187501L, "RP_EXP_10");
            Long lineId = settlement.getLines().getFirst().getId();

            assertThatThrownBy(() -> processService.process(settlement.getId(), lineId))
                    .isInstanceOfSatisfying(DomainException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PlayerError.PLAYER_NOT_FOUND)
                    );

            assertThat(growthChangeCount()).isZero();
            assertThat(lineStatus(lineId)).isEqualTo(RewardSettlementLineStatus.FAILED.name());
            assertThat(lineFailureCode(lineId)).isEqualTo(PlayerError.PLAYER_NOT_FOUND.code());
        }

        @Test
        @DisplayName("ITEM Line을 EXP processor로 호출하면 known failure로 기록한다")
        void recordsWrongRewardType() {
            insertPlayer(0L);
            RewardSettlement settlement = createSettlement(187502L, "RP_EXP_10");
            Long lineId = settlement.getLines().getFirst().getId();
            jdbcTemplate.update("""
                    UPDATE reward_settlement_lines
                    SET reward_type = 'ITEM', item_id = 77
                    WHERE id = ?
                    """, lineId);

            assertThatThrownBy(() -> processService.process(settlement.getId(), lineId))
                    .isInstanceOfSatisfying(DomainException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(RewardError.REWARD_SETTLEMENT_LINE_NOT_EXP)
                    );

            assertThat(playerExp()).isZero();
            assertThat(growthChangeCount()).isZero();
            assertThat(lineStatus(lineId)).isEqualTo(RewardSettlementLineStatus.FAILED.name());
            assertThat(lineFailureCode(lineId))
                    .isEqualTo(RewardError.REWARD_SETTLEMENT_LINE_NOT_EXP.code());
        }

        @Test
        @DisplayName("늦은 failure recorder는 이미 SUCCEEDED인 Line을 덮어쓰지 않는다")
        void doesNotOverwriteSucceededLine() {
            insertPlayer(0L);
            RewardSettlement settlement = createSettlement(187503L, "RP_EXP_10");
            Long lineId = settlement.getLines().getFirst().getId();
            processService.process(settlement.getId(), lineId);

            failureRecorder.record(
                    settlement.getId(),
                    lineId,
                    PlayerError.PLAYER_GROWTH_CHANGE_INCONSISTENT
            );

            assertThat(lineStatus(lineId)).isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
            assertThat(lineFailureCode(lineId)).isNull();
            assertThat(playerExp()).isEqualTo(10L);
            assertThat(growthChangeCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("RP_NONE으로 무보상 Settlement를 생성할 때")
    class CreateNoRewardSettlement {

        @Test
        @DisplayName("Line 없이 즉시 COMPLETED가 되고 같은 Source 재호출은 기존 행을 반환한다")
        void completesAndReplaysWithoutLines() {
            RewardSettlement first = createSettlement(189001L, "RP_NONE");
            RewardSettlement replay = createSettlement(189001L, "RP_NONE");

            assertThat(first.getLines()).isEmpty();
            assertThat(first.getStatus()).isEqualTo(RewardSettlementStatus.COMPLETED);
            assertThat(replay.getId()).isEqualTo(first.getId());
            assertThat(replay.getLines()).isEmpty();
            assertThat(replay.getStatus()).isEqualTo(RewardSettlementStatus.COMPLETED);
            assertThat(settlementRowCount()).isEqualTo(1);
            assertThat(settlementLineRowCount()).isZero();
        }
    }

    @Nested
    @DisplayName("알려진 실패를 명시적으로 Retry 준비할 때")
    class PrepareKnownFailureRetry {

        @Test
        @DisplayName("FAILED를 PENDING으로 영속화한 뒤 명시적 processor 호출에서 EXP를 한 번 지급한다")
        void preparesThenProcessesExactlyOnce() {
            RewardSettlement settlement = createSettlement(189101L, "RP_EXP_10");
            Long lineId = settlement.getLines().getFirst().getId();
            assertThatThrownBy(() -> processService.process(settlement.getId(), lineId))
                    .isInstanceOfSatisfying(DomainException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(PlayerError.PLAYER_NOT_FOUND)
                    );
            insertPlayer(0L);

            RewardSettlementLineRetryPreparationResult prepared =
                    retryPreparationService.prepare(settlement.getId(), lineId);

            assertThat(prepared.changed()).isTrue();
            assertThat(lineStatus(lineId)).isEqualTo(RewardSettlementLineStatus.PENDING.name());
            assertThat(lineFailureCode(lineId)).isNull();
            assertThat(settlementStatus(settlement.getId()))
                    .isEqualTo(RewardSettlementStatus.PENDING.name());
            assertThat(playerExp()).isZero();
            assertThat(growthChangeCount()).isZero();

            RewardSettlementExpProcessResult processed =
                    processService.process(settlement.getId(), lineId);

            assertThat(processed.replayed()).isFalse();
            assertThat(playerExp()).isEqualTo(10L);
            assertThat(growthChangeCount()).isEqualTo(1);
            assertThat(growthChangeCountByRewardLine(lineId)).isEqualTo(1);
            assertThat(lineStatus(lineId)).isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
            assertThat(settlementStatus(settlement.getId()))
                    .isEqualTo(RewardSettlementStatus.COMPLETED.name());
        }

        @Test
        @DisplayName("같은 FAILED Line의 두 동시 요청은 한 번만 변경하고 최종 PENDING을 유지한다")
        void preparesSameFailedLineConcurrently() throws Exception {
            RewardSettlement settlement = createSettlement(189102L, "RP_EXP_10");
            Long lineId = settlement.getLines().getFirst().getId();
            assertThatThrownBy(() -> processService.process(settlement.getId(), lineId))
                    .isInstanceOf(DomainException.class);

            List<RewardSettlementLineRetryPreparationResult> results = runConcurrently(
                    () -> retryPreparationService.prepare(settlement.getId(), lineId),
                    () -> retryPreparationService.prepare(settlement.getId(), lineId)
            );

            assertThat(results)
                    .extracting(RewardSettlementLineRetryPreparationResult::changed)
                    .containsExactlyInAnyOrder(true, false);
            assertThat(lineStatus(lineId)).isEqualTo(RewardSettlementLineStatus.PENDING.name());
            assertThat(lineFailureCode(lineId)).isNull();
            assertThat(settlementStatus(settlement.getId()))
                    .isEqualTo(RewardSettlementStatus.PENDING.name());
            assertThat(growthChangeCount()).isZero();
        }
    }

    private RewardSettlement createSettlement(long sourceId, String profileCode) {
        return createService.create(PLAYER_ID, SOURCE_TYPE, sourceId, profileCode);
    }

    private void insertPlayer(long exp) {
        int level = exp >= 100 ? 2 : 1;
        jdbcTemplate.update("""
                INSERT INTO player (
                    id, user_id, name, gender, level, exp,
                    hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat,
                    extra_stats, status_effects, version, created_at, updated_at
                ) VALUES (
                    ?, ?, 'Reward Tester', 'male', ?, ?,
                    100, 100, 50, 50,
                    1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID, PLAYER_ID + 100000L, level, exp);
    }

    private <T> List<T> runConcurrently(
            CheckedSupplier<T> first,
            CheckedSupplier<T> second
    ) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<T> firstFuture = executor.submit(() -> {
            ready.countDown();
            start.await();
            return first.get();
        });
        Future<T> secondFuture = executor.submit(() -> {
            ready.countDown();
            start.await();
            return second.get();
        });
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        return List.of(
                firstFuture.get(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS),
                secondFuture.get(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS)
        );
    }

    private long playerExp() {
        return jdbcTemplate.queryForObject(
                "SELECT exp FROM player WHERE id = ?", Long.class, PLAYER_ID
        );
    }

    private int playerLevel() {
        return jdbcTemplate.queryForObject(
                "SELECT level FROM player WHERE id = ?", Integer.class, PLAYER_ID
        );
    }

    private int growthChangeCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_growth_changes", Integer.class
        );
    }

    private int growthChangeCountByRewardLine(Long lineId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_growth_changes WHERE reward_line_id = ?",
                Integer.class,
                lineId
        );
    }

    private String lineStatus(Long lineId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM reward_settlement_lines WHERE id = ?", String.class, lineId
        );
    }

    private String lineFailureCode(Long lineId) {
        return jdbcTemplate.queryForObject(
                "SELECT failure_code FROM reward_settlement_lines WHERE id = ?", String.class, lineId
        );
    }

    private String settlementStatus(Long settlementId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM reward_settlements WHERE id = ?", String.class, settlementId
        );
    }

    private int settlementRowCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reward_settlements", Integer.class);
    }

    private int settlementLineRowCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reward_settlement_lines", Integer.class);
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get();
    }

    static final class LevelUpCommitProbe {
        private final AtomicInteger count = new AtomicInteger();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void on(PlayerLeveledUp event) {
            count.incrementAndGet();
        }

        int count() {
            return count.get();
        }

        void reset() {
            count.set(0);
        }
    }

    @TestConfiguration
    static class EventProbeConfig {
        @Bean
        LevelUpCommitProbe levelUpCommitProbe() {
            return new LevelUpCommitProbe();
        }
    }
}
