package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.Wallet;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.repository.WalletRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class WalletReader {

    private final WalletRepository walletRepository;

    public Wallet getWallet(Long ownerId) {
        return walletRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new DomainException(EconomyError.WALLET_NOT_FOUND));
    }

    public Wallet getForUpdate(Long ownerId) {
        return walletRepository.findByOwnerIdForUpdate(ownerId)
                .orElseThrow(() -> new DomainException(EconomyError.WALLET_NOT_FOUND));
    }

    public Optional<Wallet> find(Long ownerId) {
        return walletRepository.findByOwnerId(ownerId);
    }

    public Optional<Wallet> findForUpdate(Long ownerId) {
        return walletRepository.findByOwnerIdForUpdate(ownerId);
    }
}
