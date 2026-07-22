package online.lifeasgame.reward.application.query;

import java.util.List;

public interface RewardProfileQueryRepository {

    List<RewardProfileSummaryView> findActiveSummaries();
}
