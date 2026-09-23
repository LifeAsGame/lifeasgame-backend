package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.application.internal.RewardGoldCreditApi;
import online.lifeasgame.economy.domain.Currency;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.Wallet;
import online.lifeasgame.economy.infra.RewardGoldCreditStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RewardGoldCreditor implements RewardGoldCreditApi {
    private final WalletReader walletReader;
    private final WalletWriter walletWriter;
    private final RewardGoldCreditStore credits;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Receipt credit(Long lineId, Long playerId, Long accountId, long amount) {
        if (lineId == null || lineId <= 0 || playerId == null || playerId <= 0
                || accountId == null || accountId <= 0 || amount <= 0) {
            throw new DomainException(EconomyError.REWARD_CREDIT_INVALID);
        }
        credits.ensureWallet(playerId);
        Wallet wallet = walletReader.getByOwnerIdForUpdate(playerId).orElseThrow();
        Receipt receipt = new Receipt(lineId, playerId, accountId, amount);
        Optional<Receipt> existing = credits.find(lineId);
        if (existing.isPresent()) {
            if (!existing.get().equals(receipt)) {
                throw new DomainException(EconomyError.REWARD_CREDIT_CONFLICT);
            }
            return existing.get();
        }
        wallet.deposit(Money.of(amount, Currency.GOLD));
        walletWriter.save(wallet);
        credits.save(receipt);
        return receipt;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public Optional<Receipt> find(Long lineId) {
        return credits.find(lineId);
    }
}
