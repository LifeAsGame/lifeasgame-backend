package online.lifeasgame.reward.application.query;

import online.lifeasgame.reward.domain.RewardProfileStatus;

public record RewardProfileSummaryView(
        Long id,
        String code,
        String name,
        RewardProfileStatus status
) {
}
