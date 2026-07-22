package online.lifeasgame.reward.application;

import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("RewardSettlement 동시 생성 복구")
class RewardSettlementConcurrencyTest {

    private static final Long PLAYER_ID = 185L;
    private static final Long SOURCE_ID = 185002L;
    private static final String PROFILE_CODE = "RP_EXP_10";
    private static final RewardSettlementSourceType SOURCE_TYPE =
            RewardSettlementSourceType.QUEST_COMPLETION;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_settlement_concurrency")
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
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RewardSettlementReader settlementReader;

    @Autowired
    private RewardSettlementCreateAttempt createAttempt;

    @Autowired
    private RewardSettlementCreateService createService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("상위 REPEATABLE READ 트랜잭션이 최초 조회 snapshot을 유지할 때")
    class RecoverAfterUniqueConflict {

        @Test
        @DisplayName("Unique 충돌 후 새 조회 트랜잭션에서 승자 Settlement를 반환한다")
        void returnsWinnerFromNewReadTransaction() {
            TransactionTemplate repeatableRead = new TransactionTemplate(transactionManager);
            repeatableRead.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

            CompetitionResult result = repeatableRead.execute(status -> {
                assertThat(settlementReader.findByIdentity(PLAYER_ID, SOURCE_TYPE, SOURCE_ID))
                        .isEmpty();

                RewardSettlement savedWinner = createAttempt.create(
                        PLAYER_ID,
                        SOURCE_TYPE,
                        SOURCE_ID,
                        PROFILE_CODE
                );

                boolean ordinaryReadStillMisses = settlementReader
                        .findByIdentity(PLAYER_ID, SOURCE_TYPE, SOURCE_ID)
                        .isEmpty();
                RewardSettlement recovered = createService.create(
                        PLAYER_ID,
                        SOURCE_TYPE,
                        SOURCE_ID,
                        PROFILE_CODE
                );

                return new CompetitionResult(
                        savedWinner.getId(),
                        recovered.getId(),
                        savedWinner.getLines().size(),
                        recovered.getLines().size(),
                        ordinaryReadStillMisses
                );
            });

            assertThat(result).isNotNull();
            assertThat(result.ordinaryReadStillMisses()).isTrue();
            assertThat(result.recoveredId()).isEqualTo(result.winnerId());
            assertThat(result.winnerLineCount()).isEqualTo(1);
            assertThat(result.recoveredLineCount()).isEqualTo(1);
            assertThat(settlementRowCount()).isEqualTo(1);
            assertThat(settlementLineRowCount()).isEqualTo(1);
        }
    }

    private int settlementRowCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM reward_settlements
                WHERE player_id = ?
                  AND source_type = ?
                  AND source_id = ?
                """, Integer.class, PLAYER_ID, SOURCE_TYPE.name(), SOURCE_ID);
    }

    private int settlementLineRowCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM reward_settlement_lines line
                JOIN reward_settlements settlement
                  ON settlement.id = line.reward_settlement_id
                WHERE settlement.player_id = ?
                  AND settlement.source_type = ?
                  AND settlement.source_id = ?
                """, Integer.class, PLAYER_ID, SOURCE_TYPE.name(), SOURCE_ID);
    }

    private record CompetitionResult(
            Long winnerId,
            Long recoveredId,
            int winnerLineCount,
            int recoveredLineCount,
            boolean ordinaryReadStillMisses
    ) {
    }
}
