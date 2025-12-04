package online.lifeasgame.economy.domain.repository;

import java.util.Optional;
import online.lifeasgame.economy.domain.Wallet;

public interface WalletRepository {
    Optional<Wallet> findByOwnerId(Long ownerId);
    Optional<Wallet> findByOwnerIdForUpdate(Long ownerId);
    Wallet save(Wallet wallet);
}
