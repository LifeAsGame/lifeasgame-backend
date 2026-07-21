package online.lifeasgame.reward.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.repository.RewardProfileRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RewardProfileRepositoryAdapter implements RewardProfileRepository {

    private final JpaRewardProfileRepository jpaRepository;

    @Override
    public RewardProfile save(RewardProfile rewardProfile) {
        return jpaRepository.save(rewardProfile);
    }

    @Override
    public Optional<RewardProfile> findByCode(String code) {
        return jpaRepository.findByCode(code);
    }
}
