package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.error.RewardError;
import online.lifeasgame.reward.domain.repository.RewardProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class RewardProfileReader {

    private final RewardProfileRepository repository;

    public RewardProfile getActiveByCodeOrThrow(String code) {
        RewardProfile profile = getByCodeOrThrow(code);
        profile.assertActive();
        return profile;
    }

    public RewardProfile getByCodeOrThrow(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new DomainException(RewardError.REWARD_PROFILE_NOT_FOUND));
    }
}
