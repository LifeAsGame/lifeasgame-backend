package online.lifeasgame.reward.application.result;

import online.lifeasgame.reward.domain.RewardDefinition;

public final class RewardDefinitionResult {

    private RewardDefinitionResult() {
    }

    public record Detail(
            Long id,
            String code,
            String name,
            String rewardType,
            Long amount,
            Long itemId,
            boolean active
    ) {
        public static Detail from(RewardDefinition definition) {
            return new Detail(
                    definition.getId(),
                    definition.getCode(),
                    definition.getName(),
                    definition.getRewardType().name(),
                    definition.getAmount(),
                    definition.getItemId(),
                    definition.isActive()
            );
        }
    }
}
