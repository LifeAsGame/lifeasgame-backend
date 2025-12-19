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

    private final WalletRepository repository;

    public Wallet save(Wallet wallet) {
        return repository.save(wallet);
    }
}
