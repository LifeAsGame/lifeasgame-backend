package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.application.internal.RewardProfileLookupApi;
import online.lifeasgame.reward.domain.RewardProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class RewardProfileLookupService implements RewardProfileLookupApi {

    private final RewardProfileReader rewardProfileReader;

    @Override
    public RewardProfileReference getActiveByCode(String code) {
        RewardProfile profile = rewardProfileReader.getActiveByCodeOrThrow(code);
        return new RewardProfileReference(profile.getCode());
    }
}
