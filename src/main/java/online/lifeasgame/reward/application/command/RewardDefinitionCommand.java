package online.lifeasgame.reward.application.command;

import online.lifeasgame.reward.domain.RewardType;

public final class RewardDefinitionCommand {

    private RewardDefinitionCommand() {
    }

    public record Create(
            String code,
            String name,
            RewardType rewardType,
            Long amount,
            Long itemId,
            boolean active
    ) {
    }

    public record Update(
            String code,
            String name,
            RewardType rewardType,
            Long amount,
            Long itemId,
            boolean active
    ) {
    }
}
