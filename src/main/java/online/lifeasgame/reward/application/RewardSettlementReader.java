package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import online.lifeasgame.reward.domain.error.RewardError;
import online.lifeasgame.reward.domain.repository.RewardSettlementRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class RewardSettlementReader {

    private final RewardSettlementRepository repository;

    public Optional<RewardSettlement> findByIdentity(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId
    ) {
        return repository.findByIdentity(playerId, sourceType, sourceId);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<RewardSettlement> findByIdentityInNewTransaction(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId
    ) {
        return repository.findByIdentity(playerId, sourceType, sourceId);
    }

    public RewardSettlement getByIdentityOrThrow(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId
    ) {
        return findByIdentity(playerId, sourceType, sourceId)
                .orElseThrow(() -> new DomainException(RewardError.REWARD_SETTLEMENT_NOT_FOUND));
    }

    public RewardSettlement getByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new DomainException(RewardError.REWARD_SETTLEMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public RewardSettlement getByIdInNewTransactionOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new DomainException(
                        RewardError.REWARD_SETTLEMENT_NOT_FOUND
                ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public RewardSettlement getByIdForUpdateOrThrow(Long id) {
        return findByIdForUpdate(id)
                .orElseThrow(() -> new DomainException(RewardError.REWARD_SETTLEMENT_NOT_FOUND));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<RewardSettlement> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id);
    }
}
