package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.InventoryRewardDeliveryApi;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.reward.domain.*;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(properties = "app.quest.definition-bootstrap.enabled=true")
@ActiveProfiles({"test", "migration-test"})
@DisplayName("모험의 준비 계정 보상 MySQL 트랜잭션")
class AdventureRewardIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("adventure_rewards").withUsername("test").withPassword("test");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired RewardSettlementCreateService creator;
    @Autowired QuestCompletionRewardService completion;
    @Autowired RewardSettlementGoldProcessor gold;
    @Autowired RewardSettlementItemProcessService item;
    @Autowired RewardSettlementLineRetryPreparationService retry;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean RewardSettlementWriter writer;
    @MockitoSpyBean InventoryRewardDeliveryApi inventory;
    @MockitoSpyBean online.lifeasgame.economy.infra.RewardGoldCreditStore credits;
    static final long PLAYER = 360001L;
    static final long ACCOUNT = 360101L;

    @BeforeEach
    void setup() {
        for (String table : new String[]{"reward_gold_credits", "account_reward_entitlements",
                "inventory_reward_deliveries", "mailbox_entries", "player_mailbox", "player_inventory",
                "reward_settlement_lines", "reward_settlements", "wallet_balances", "wallets"}) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("DELETE FROM player WHERE user_id = ?", ACCOUNT);
        insertPlayer(PLAYER);
    }

    @Test
    @DisplayName("동시 완료와 재전달에서도 계정당 GOLD 100과 비귀속 결정 하나만 지급한다")
    void concurrentCompletions() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); completion.process(fact(PLAYER, 1)); return true; });
            var second = executor.submit(() -> { start.await(); completion.process(fact(PLAYER, 2)); return true; });
            start.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        }
        completion.process(fact(PLAYER, 1));
        completion.process(fact(PLAYER, 2));
        assertThat(balance()).isEqualTo(100);
        assertThat(count("reward_gold_credits")).isEqualTo(1);
        assertThat(count("inventory_reward_deliveries")).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT status FROM reward_settlements", String.class))
                .containsExactlyInAnyOrder("COMPLETED", "NOT_ELIGIBLE");
        assertThat(jdbc.queryForObject("SELECT bound+0 FROM mailbox_entries", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT quantity FROM mailbox_entries", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Player 재생성 및 표시 버전 변경 후 다른 요청도 동일 계정 자격을 재설정하지 않는다")
    void recreatedPlayerKeepsSpentEntitlement() {
        completion.process(fact(PLAYER, 3));
        jdbc.update("DELETE FROM player WHERE id = ?", PLAYER);
        insertPlayer(PLAYER + 1);
        assertThat(jdbc.update("UPDATE quests SET definition_version = 2 WHERE code = 'Q_ADVENTURE_PREPARATION'")).isEqualTo(1);
        completion.process(fact(PLAYER + 1, 4));
        assertThat(balance()).isEqualTo(100);
        assertThat(count("reward_gold_credits")).isEqualTo(1);
        assertThat(count("inventory_reward_deliveries")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM reward_settlements WHERE player_id = ?",
                String.class, PLAYER + 1)).isEqualTo("NOT_ELIGIBLE");
    }

    @Test
    @DisplayName("입금 후 line 저장 실패는 Wallet과 receipt를 모두 rollback하며 재시도는 한 번 지급한다")
    void atomicCreditAndLine() {
        var settlement = create(5);
        var line = line(settlement, RewardType.GOLD);
        doThrow(new RuntimeException("line save failure")).when(writer)
                .saveAndFlush(argThat(s -> s.getId().equals(settlement.getId())));
        assertThatThrownBy(() -> gold.process(settlement.getId(), line.getId())).hasMessage("line save failure");
        assertThat(balance()).isZero();
        assertThat(count("reward_gold_credits")).isZero();
        assertThat(lineStatus(line)).isEqualTo("PENDING");
        reset(writer);
        gold.process(settlement.getId(), line.getId());
        gold.process(settlement.getId(), line.getId());
        assertThat(balance()).isEqualTo(100);
        assertThat(count("reward_gold_credits")).isEqualTo(1);
    }

    @Test
    @DisplayName("동일 GOLD line 동시 요청은 Wallet에 한 번만 입금한다")
    void concurrentSameLine() throws Exception {
        var settlement = create(6);
        var line = line(settlement, RewardType.GOLD);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { start.await(); gold.process(settlement.getId(), line.getId()); return true; });
            var b = executor.submit(() -> { start.await(); gold.process(settlement.getId(), line.getId()); return true; });
            start.countDown();
            a.get(30, TimeUnit.SECONDS);
            b.get(30, TimeUnit.SECONDS);
        }
        assertThat(balance()).isEqualTo(100);
        assertThat(count("reward_gold_credits")).isEqualTo(1);
    }

    @Test
    @DisplayName("GOLD 성공 Item 실패 후 재전달 및 실패 line 재시도는 GOLD를 재지급하지 않는다")
    void goldSuccessItemFailure() {
        doThrow(new DomainException(InventoryError.MAILBOX_FULL)).when(inventory)
                .deliverReward(anyLong(), anyLong(), eq("IT_RECORD_CRYSTAL"), anyLong());
        completion.process(fact(PLAYER, 7));
        var settlement = create(7);
        assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.PARTIAL_FAILED);
        assertThat(balance()).isEqualTo(100);
        completion.process(fact(PLAYER, 7));
        reset(inventory);
        retry.prepare(settlement.getId(), line(settlement, RewardType.ITEM).getId());
        completion.process(fact(PLAYER, 7));
        assertCompletedOnce(7);
    }

    @Test
    @DisplayName("Item 성공 GOLD 실패 후 재시도는 Item을 재지급하지 않는다")
    void itemSuccessGoldFailure() {
        var settlement = create(8);
        item.process(settlement.getId(), line(settlement, RewardType.ITEM).getId());
        var goldLine = line(settlement, RewardType.GOLD);
        doThrow(new DomainException(RewardError.REWARD_GOLD_RECEIPT_INCONSISTENT)).when(credits)
                .save(any());
        assertThatThrownBy(() -> gold.process(settlement.getId(), goldLine.getId())).isInstanceOf(DomainException.class);
        assertThat(create(8).getStatus()).isEqualTo(RewardSettlementStatus.PARTIAL_FAILED);
        reset(credits);
        retry.prepare(settlement.getId(), goldLine.getId());
        completion.process(fact(PLAYER, 8));
        assertCompletedOnce(8);
    }

    @Test
    @DisplayName("성공 receipt 유실을 재입금으로 복구하지 않고 기존 기념품은 귀속을 유지한다")
    void missingReceiptAndBoundSouvenir() {
        var settlement = create(9);
        var goldLine = line(settlement, RewardType.GOLD);
        gold.process(settlement.getId(), goldLine.getId());
        jdbc.update("DELETE FROM reward_gold_credits");
        assertThatThrownBy(() -> gold.process(settlement.getId(), goldLine.getId())).isInstanceOf(DomainException.class);
        assertThat(balance()).isEqualTo(100);
        var souvenir = creator.create(PLAYER, RewardSettlementSourceType.QUEST_COMPLETION, 10L, "RP_EXP_AND_ITEM_FIRST_STEP_20");
        item.process(souvenir.getId(), line(souvenir, RewardType.ITEM).getId());
        assertThat(jdbc.queryForObject("SELECT bound+0 FROM mailbox_entries", Integer.class)).isEqualTo(1);
    }

    void assertCompletedOnce(long source) {
        assertThat(create(source).getStatus()).isEqualTo(RewardSettlementStatus.COMPLETED);
        assertThat(balance()).isEqualTo(100);
        assertThat(count("reward_gold_credits")).isEqualTo(1);
        assertThat(count("inventory_reward_deliveries")).isEqualTo(1);
    }
    RewardSettlement create(long source) {
        return creator.create(PLAYER, RewardSettlementSourceType.QUEST_COMPLETION, source, "RP_ADVENTURE_PREPARATION");
    }
    RewardSettlementLine line(RewardSettlement settlement, RewardType type) {
        return settlement.getLines().stream().filter(l -> l.getRewardType() == type).findFirst().orElseThrow();
    }
    String lineStatus(RewardSettlementLine line) {
        return jdbc.queryForObject("SELECT status FROM reward_settlement_lines WHERE id = ?", String.class, line.getId());
    }
    long balance() {
        return jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM wallet_balances WHERE currency = 'GOLD'", Long.class);
    }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    QuestRewardReadyFact fact(long player, long source) {
        return new QuestRewardReadyFact(1, player, source, "RP_ADVENTURE_PREPARATION", 1L,
                "Q_ADVENTURE_PREPARATION", 1, Instant.now(), "adventure:" + source, "모험의 준비");
    }
    void insertPlayer(long id) {
        jdbc.update("""
                INSERT INTO player (id, user_id, name, gender, level, exp, hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat, extra_stats, status_effects,
                    version, created_at, updated_at)
                VALUES (?, ?, 'Adventure', 'male', 1, 0, 100, 100, 50, 50, 1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, ACCOUNT);
    }
}
