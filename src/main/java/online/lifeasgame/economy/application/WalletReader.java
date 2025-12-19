package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Wallet;
import online.lifeasgame.economy.domain.repository.WalletRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class WalletReader {

    private final WalletRepository repository;

    public Optional<Wallet> getByOwnerId(Long ownerId) {
        return repository.findByOwnerId(ownerId);
    }

    public Optional<Wallet> getByOwnerIdForUpdate(Long ownerId) {
        return repository.findByOwnerIdForUpdate(ownerId);
    }
}
