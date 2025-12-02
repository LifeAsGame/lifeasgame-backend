package online.lifeasgame.economy.application;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.WalletHold;
import online.lifeasgame.economy.domain.repository.WalletHoldRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class WalletHoldReader {

    private final WalletHoldRepository walletHoldRepository;

    public Optional<WalletHold> findByHoldId(String holdId) {
        return walletHoldRepository.findByHoldId(holdId);
    }
}
