package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
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

    public Wallet getOrCreateForUpdate(Long ownerId) {
        return walletRepository.findByOwnerIdForUpdate(ownerId)
                .orElseGet(() -> walletRepository.save(Wallet.open(ownerId)));
    }

    public Wallet getOrCreate(Long ownerId) {
        return walletRepository.findByOwnerId(ownerId)
                .orElseGet(() -> walletRepository.save(Wallet.open(ownerId)));
    }

    public Wallet save(Wallet wallet) {
        return walletRepository.save(wallet);
    }
}
