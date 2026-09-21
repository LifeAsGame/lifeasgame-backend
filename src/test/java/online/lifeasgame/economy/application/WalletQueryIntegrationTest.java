package online.lifeasgame.economy.application;

import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.Currency;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.Wallet;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;

@Testcontainers
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles({"test", "migration-test"})
@DisplayName("통화별 Wallet read-only MySQL 조회 계약")
class WalletQueryIntegrationTest {

    private static final long PLAYER = 35201L;
    private static final long OTHER = 35202L;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("wallet_query").withUsername("lifeasgame").withPassword("lifeasgame");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired WalletQueryService queryService;
    @Autowired WalletReader walletReader;
    @Autowired WalletWriter walletWriter;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean DomainEventPublisher publisher;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM wallet_holds");
        jdbc.update("DELETE FROM wallet_balances");
        jdbc.update("DELETE FROM wallets");
    }

    @Test
    @DisplayName("지갑이 없어도 GOLD·GEM 0을 순서대로 반환하며 반복 조회가 지갑을 생성하지 않는다")
    void absentWalletStaysAbsent() {
        assertRead(PLAYER, 0, 0, 0, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM wallets", Long.class)).isZero();
    }

    @Test
    @DisplayName("빈 지갑·GEM만 있는 지갑의 누락 통화는 0이며 조회가 balance를 생성하지 않는다")
    void missingBalancesStayAbsent() {
        createWallet(PLAYER, wallet -> {});
        assertRead(PLAYER, 0, 0, 0, 0);
        mutate(PLAYER, wallet -> wallet.deposit(Money.of(50, Currency.GEM)));
        assertRead(PLAYER, 0, 0, 50, 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM wallet_balances", Long.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Currency.class)
    @DisplayName("hold 합계는 통화·소유자를 격리하고 확정·취소·만료 명령이 정산한 상태만 반영한다")
    void followsPersistedSettlement(Currency currency) {
        Currency otherCurrency = currency == Currency.GOLD ? Currency.GEM : Currency.GOLD;
        createWallet(PLAYER, wallet -> {
            wallet.deposit(Money.of(100, currency));
            wallet.deposit(Money.of(50, otherCurrency));
        });
        createWallet(OTHER, wallet -> {
            wallet.deposit(Money.of(900, currency));
            wallet.placeHold(Money.of(400, currency), "other owner", Instant.now(), 3600);
        });
        Instant past = Instant.now().minusSeconds(120);
        var tx = new TransactionTemplate(transactionManager);
        List<String> holds = tx.execute(status -> {
            Wallet wallet = walletReader.getByOwnerIdForUpdate(PLAYER).orElseThrow();
            return List.of(
                    wallet.placeHold(Money.of(20, currency), "confirm", Instant.now(), 3600),
                    wallet.placeHold(Money.of(5, currency), "cancel", Instant.now(), 3600),
                    wallet.placeHold(Money.of(3, currency), "expired but OPEN", past, 30),
                    wallet.placeHold(Money.of(7, otherCurrency), "other currency", Instant.now(), 3600));
        });
        assertCurrencyRead(currency, 72, 28, 43, 7);
        mutate(PLAYER, wallet -> wallet.commitHold(holds.get(0)));
        assertCurrencyRead(currency, 72, 8, 43, 7);
        mutate(PLAYER, wallet -> wallet.cancelHold(holds.get(1)));
        assertCurrencyRead(currency, 77, 3, 43, 7);
        mutate(PLAYER, wallet -> wallet.expireHold(holds.get(2), Instant.now()));
        assertCurrencyRead(currency, 80, 0, 43, 7);
    }

    @Test
    @DisplayName("hold 개수와 관계없이 두 집계 조회만 실행하고 엔티티·컬렉션을 로딩하지 않는다")
    void projectsWithoutLoadingHoldHistory() {
        createWallet(PLAYER, wallet -> {
            wallet.deposit(Money.of(1000, Currency.GOLD));
            for (int i = 0; i < 40; i++) {
                String hold = wallet.placeHold(Money.of(1, Currency.GOLD), "history", Instant.now(), 3600);
                if (i % 2 == 0) wallet.commitHold(hold);
            }
        });
        assertRead(PLAYER, 960, 20, 0, 0);
    }

    private void assertCurrencyRead(Currency currency, long available, long held, long otherAvailable, long otherHeld) {
        if (currency == Currency.GOLD) assertRead(PLAYER, available, held, otherAvailable, otherHeld);
        else assertRead(PLAYER, otherAvailable, otherHeld, available, held);
    }

    private void assertRead(long player, long gold, long goldHeld, long gem, long gemHeld) {
        var before = snapshot();
        clearInvocations(publisher);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        for (int i = 0; i < 3; i++) {
            var result = queryService.wallet(player);
            assertThat(result.amount()).isEqualTo(gold);
            assertThat(result.currency()).isEqualTo("GOLD");
            assertThat(result.balances()).containsExactly(
                    new EconomyResult.CurrencyBalance(Currency.GOLD, gold, goldHeld),
                    new EconomyResult.CurrencyBalance(Currency.GEM, gem, gemHeld));
        }
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(6);
        assertThat(statistics.getEntityLoadCount()).isZero();
        assertThat(statistics.getCollectionLoadCount()).isZero();
        assertThat(snapshot()).isEqualTo(before);
        verifyNoInteractions(publisher);
    }

    private void createWallet(long owner, Consumer<Wallet> action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Wallet wallet = Wallet.open(owner);
            action.accept(wallet);
            walletWriter.save(wallet);
        });
    }

    private void mutate(long owner, Consumer<Wallet> action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                action.accept(walletReader.getByOwnerIdForUpdate(owner).orElseThrow()));
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        var result = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (String table : List.of("wallets", "wallet_balances", "wallet_holds", "outbox_events")) {
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"));
        }
        return result;
    }
}
