package online.lifeasgame.economy.infra;

import java.util.Optional;
import online.lifeasgame.economy.domain.WalletHold;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaWalletHoldRepository extends JpaRepository<WalletHold, Long> {
    Optional<WalletHold> findByHoldId(String holdId);
}
