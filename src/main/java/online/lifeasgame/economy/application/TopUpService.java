package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.port.PaymentGateway;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.Currency;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.Wallet;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.domain.event.EconomyEventType;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TopUpService {

    private final WalletReader walletReader;
    private final WalletWriter walletWriter;
    private final PaymentGateway paymentGateway;
    private final IdempotencyKeyStore idempotencyKeyStore;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public void topUp(Long ownerId, EconomyCommand.TopUp command) {
        Currency currency = Currency.parseOptional(command.currency(), Currency.GOLD);
        if (!idempotencyKeyStore.acquire(command.idempotencyKey(), Duration.ofMinutes(10))) {
            throw new DomainException(EconomyError.DUPLICATE_REQUEST);
        }

        boolean ok = paymentGateway.confirmCharge(
                command.paymentKey(),
                command.orderId(),
                command.amount(),
                currency
        );

        if (!ok) {
            throw new DomainException(EconomyError.PAYMENT_REJECTED);
        }

        var wallet = lockOrCreateWallet(ownerId);
        walletWriter.deposit(wallet, Money.of(command.amount(), currency));

        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.TOPUP_COMPLETED)
                        .actorId(ownerId)
                        .attribute("orderId", command.orderId())
                        .occurredAt(java.time.Instant.now())
                        .build()
        );
    }

    @Transactional
    public EconomyResult.WalletBalance adjust(EconomyCommand.AdjustWallet command) {
        Currency currency = Currency.parseOptional(command.currency(), Currency.GOLD);
        var wallet = lockOrCreateWallet(command.playerId());
        Money money = Money.of(command.amount(), currency);
        if (command.debit()) {
            walletWriter.withdraw(wallet, money);
        } else {
            walletWriter.deposit(wallet, money);
        }

        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.WALLET_ADJUSTED)
                        .actorId(command.playerId())
                        .attribute("reason", command.reason())
                        .occurredAt(java.time.Instant.now())
                        .build()
        );
        var balance = wallet.getBalance(currency);
        return EconomyResult.WalletBalance.of(balance.available(), balance.getCurrency().name());
    }

    @Transactional
    public EconomyResult.WalletBalance wallet(Long playerId) {
        var wallet = walletReader.find(playerId)
                .orElseGet(() -> walletWriter.save(Wallet.open(playerId)));
        var balance = wallet.getBalance();
        return EconomyResult.WalletBalance.of(balance.available(), balance.getCurrency().name());
    }

    private Wallet lockOrCreateWallet(Long ownerId) {
        return walletReader.findForUpdate(ownerId)
                .orElseGet(() -> walletWriter.save(Wallet.open(ownerId)));
    }
}
