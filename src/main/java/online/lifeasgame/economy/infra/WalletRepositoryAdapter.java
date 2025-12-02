package online.lifeasgame.economy.infra;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Wallet;
import online.lifeasgame.economy.domain.repository.WalletRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryAdapter implements WalletRepository {

    private final JpaWalletRepository jpaWalletRepository;

    @Override
    public Optional<Wallet> findByOwnerId(Long ownerId) {
        return jpaWalletRepository.findByOwnerId(ownerId);
    }

    @Override
    public Optional<Wallet> findByOwnerIdForUpdate(Long ownerId) {
        return jpaWalletRepository.findByOwnerIdForUpdate(ownerId);
    }

    @Override
    public Wallet save(Wallet wallet) {
        return jpaWalletRepository.save(wallet);
    }
}
