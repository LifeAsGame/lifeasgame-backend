package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.Wallet;
import online.lifeasgame.economy.domain.repository.WalletRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class WalletWriter {

    private final WalletRepository walletRepository;

    public Wallet save(Wallet wallet) {
        return walletRepository.save(wallet);
    }

    public String placeHold(Wallet wallet, Money amount, String reason, java.time.Instant now, int ttlSeconds) {
        String holdId = wallet.placeHold(amount, reason, now, ttlSeconds);
        walletRepository.save(wallet);
        return holdId;
    }

    public void commitHold(Wallet wallet, String holdId) {
        wallet.commitHold(holdId);
        walletRepository.save(wallet);
    }

    public void withdraw(Wallet wallet, Money amount) {
        wallet.withdraw(amount);
        walletRepository.save(wallet);
    }

    public void deposit(Wallet wallet, Money amount) {
        wallet.deposit(amount);
        walletRepository.save(wallet);
    }

    public void cancelHold(Wallet wallet, String holdId) {
        wallet.cancelHold(holdId);
        walletRepository.save(wallet);
    }

    public void expireHolds(Wallet wallet, java.time.Instant now) {
        wallet.expireHolds(now);
        walletRepository.save(wallet);
    }
}
