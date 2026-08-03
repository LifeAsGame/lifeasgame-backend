package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.RewardDefinition;
import online.lifeasgame.reward.domain.error.RewardError;
import online.lifeasgame.reward.domain.repository.RewardDefinitionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class RewardDefinitionReader {

    private final RewardDefinitionRepository repository;

    public RewardDefinition getByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new DomainException(
                        RewardError.REWARD_DEFINITION_NOT_FOUND
                ));
    }

    public RewardDefinition getByCodeOrThrow(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new DomainException(RewardError.REWARD_DEFINITION_NOT_FOUND));
    }
}
