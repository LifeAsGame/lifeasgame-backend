package online.lifeasgame.character.infra.growth;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.growth.PlayerGrowthChange;
import online.lifeasgame.character.domain.growth.repository.PlayerGrowthChangeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerGrowthChangeRepositoryAdapter implements PlayerGrowthChangeRepository {

    private final JpaPlayerGrowthChangeRepository jpaRepository;

    @Override
    public PlayerGrowthChange saveAndFlush(PlayerGrowthChange change) {
        return jpaRepository.saveAndFlush(change);
    }

    @Override
    public Optional<PlayerGrowthChange> findByRewardLineId(Long rewardLineId) {
        return jpaRepository.findByRewardLineId(rewardLineId);
    }
}
