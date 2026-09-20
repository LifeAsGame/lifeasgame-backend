package online.lifeasgame.economy.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.domain.Currency;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.Wallet;
import online.lifeasgame.economy.domain.WalletHold;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.repository.WalletRepository;
import online.lifeasgame.economy.infra.TossPaymentGateway;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("미검증 충전을 처리할 때")
class TopUpServiceTest {

    private static final long PLAYER_ID = 34402L;
    private static final Duration TTL = Duration.ofMinutes(10);

    @Mock
    private WalletRepository repository;
    @Mock
    private IdempotencyKeyStore idempotencyKeyStore;
    @Mock
    private DomainEventPublisher publisher;

    private TossPaymentGateway gateway;
    private TopUpService service;

    @BeforeEach
    void setUp() {
        gateway = spy(new TossPaymentGateway());
        service = service(repository);
    }

    @ParameterizedTest
    @CsvSource({"true,GOLD", "true,GEM", "false,GOLD", "false,GEM"})
    @DisplayName("기존·없는 지갑 모두 새 요청을 거절하고 잔액·hold·발행을 건드리지 않는다")
    void rejectsWithoutWalletEffects(boolean existing, Currency currency) {
        Wallet wallet = Wallet.open(PLAYER_ID);
        wallet.deposit(Money.of(100L, Currency.GOLD));
        wallet.deposit(Money.of(50L, Currency.GEM));
        Instant now = Instant.parse("2026-09-20T00:00:00Z");
        String holdId = wallet.placeHold(Money.of(10L, currency), "synthetic-hold", now, 600);
        long gold = wallet.getBalance(Currency.GOLD).available();
        long gem = wallet.getBalance(Currency.GEM).available();
        WalletRepository availableRepository = mock(WalletRepository.class, invocation ->
                switch (invocation.getMethod().getName()) {
                    case "findByOwnerId", "findByOwnerIdForUpdate" -> existing ? Optional.of(wallet) : Optional.empty();
                    case "save" -> invocation.getArgument(0);
                    default -> org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
                });
        TopUpService subject = service(availableRepository);

        for (String identity : List.of("A", "B")) {
            when(idempotencyKeyStore.acquire(identity, TTL)).thenReturn(true);
            assertThatThrownBy(() -> subject.topUp(PLAYER_ID, command(identity, currency)))
                    .isInstanceOfSatisfying(DomainException.class, error ->
                            assertThat(error.getErrorCode()).isEqualTo(EconomyError.PAYMENT_REJECTED));
            verify(idempotencyKeyStore).acquire(identity, TTL);
        }

        verifyNoInteractions(availableRepository, publisher);
        assertThat(wallet.getBalance(Currency.GOLD).available()).isEqualTo(gold);
        assertThat(wallet.getBalance(Currency.GEM).available()).isEqualTo(gem);
        assertThat((List<?>) ReflectionTestUtils.getField(wallet, "holds"))
                .singleElement().isInstanceOfSatisfying(WalletHold.class, hold -> {
                    assertThat(hold.getHoldId()).isEqualTo(holdId);
                    assertThat(hold.getAmount()).isEqualTo(10L);
                    assertThat(hold.getCurrency()).isEqualTo(currency);
                    assertThat(hold.getExpiresAt()).isEqualTo(now.plusSeconds(600));
                    assertThat(hold.isOpen()).isTrue();
                });
    }

    @Test
    @DisplayName("동일 키 중복은 거절하고 재획득해도 결제를 승인하지 않는다")
    void preservesDuplicateAndReacquisition() {
        when(idempotencyKeyStore.acquire("A", TTL)).thenReturn(true, false, true);
        for (EconomyError expected : List.of(
                EconomyError.PAYMENT_REJECTED, EconomyError.DUPLICATE_REQUEST, EconomyError.PAYMENT_REJECTED)) {
            assertThatThrownBy(() -> service.topUp(PLAYER_ID, command("A", Currency.GOLD)))
                    .isInstanceOfSatisfying(DomainException.class, error ->
                            assertThat(error.getErrorCode()).isEqualTo(expected));
        }
        verify(idempotencyKeyStore, times(3)).acquire("A", TTL);
        verify(gateway, times(2)).confirmCharge("synthetic-A", "order-A", 100L, Currency.GOLD);
        verifyNoInteractions(repository, publisher);
    }

    @Test
    @DisplayName("멱등성 저장소 장애는 그대로 전파하고 결제·지갑·발행에 진입하지 않는다")
    void preservesInfrastructureFailure() {
        var failure = new IllegalStateException("synthetic idempotency outage");
        when(idempotencyKeyStore.acquire("A", TTL)).thenThrow(failure);

        assertThatThrownBy(() -> service.topUp(PLAYER_ID, command("A", Currency.GOLD)))
                .isSameAs(failure);

        verifyNoInteractions(gateway, repository, publisher);
    }

    private TopUpService service(WalletRepository walletRepository) {
        return new TopUpService(new WalletReader(walletRepository), new WalletWriter(walletRepository),
                gateway, idempotencyKeyStore, publisher);
    }

    private EconomyCommand.TopUp command(String identity, Currency currency) {
        return new EconomyCommand.TopUp(100L, currency.name(), "synthetic-" + identity, "order-" + identity, identity);
    }
}
