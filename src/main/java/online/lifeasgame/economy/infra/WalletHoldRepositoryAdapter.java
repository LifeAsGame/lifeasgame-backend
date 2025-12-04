package online.lifeasgame.economy.infra;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.WalletHold;
import online.lifeasgame.economy.domain.repository.WalletHoldRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WalletHoldRepositoryAdapter implements WalletHoldRepository {

    private final JpaWalletHoldRepository jpaWalletHoldRepository;

    @Override
    public Optional<WalletHold> findByHoldId(String holdId) {
        return jpaWalletHoldRepository.findByHoldId(holdId);
    }
}
