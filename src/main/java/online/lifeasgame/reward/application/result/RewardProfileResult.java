package online.lifeasgame.reward.application.result;

import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.RewardProfileLine;
import online.lifeasgame.reward.application.query.RewardProfileSummaryView;

import java.util.List;

public final class RewardProfileResult {

    private RewardProfileResult() {
    }

    public record Detail(
            Long id,
            String code,
            String name,
            String status,
            List<Line> lines
    ) {
        public static Detail from(RewardProfile profile) {
            return new Detail(
                    profile.getId(),
                    profile.getCode(),
                    profile.getName(),
                    profile.getStatus().name(),
                    profile.getLines().stream().map(Line::from).toList()
            );
        }
    }

    public record Summary(Long id, String code, String name, String status) {
        public static Summary from(RewardProfileSummaryView profile) {
            return new Summary(
                    profile.id(),
                    profile.code(),
                    profile.name(),
                    profile.status().name()
            );
        }
    }

    public record Line(
            Long id,
            int sortOrder,
            Long amountOverride,
            long effectiveAmount,
            RewardDefinitionResult.Detail rewardDefinition
    ) {
        private static Line from(RewardProfileLine line) {
            return new Line(
                    line.getId(),
                    line.getSortOrder(),
                    line.getAmountOverride(),
                    line.effectiveAmount(),
                    RewardDefinitionResult.Detail.from(line.getRewardDefinition())
            );
        }
    }
}
