package online.lifeasgame.reward.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.application.query.RewardProfileQueryRepository;
import online.lifeasgame.reward.application.query.RewardProfileSummaryView;
import online.lifeasgame.reward.domain.RewardProfileStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RewardProfileQueryRepositoryAdapter implements RewardProfileQueryRepository {

    private final JpaRewardProfileQueryRepository jpaRepository;

    @Override
    public List<RewardProfileSummaryView> findActiveSummaries() {
        return jpaRepository.findSummariesByStatus(RewardProfileStatus.ACTIVE);
    }
}
