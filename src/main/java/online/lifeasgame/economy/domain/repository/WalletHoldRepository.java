package online.lifeasgame.economy.domain.repository;

import java.util.Optional;
import online.lifeasgame.economy.domain.WalletHold;

public interface WalletHoldRepository {
    Optional<WalletHold> findByHoldId(String holdId);
}
