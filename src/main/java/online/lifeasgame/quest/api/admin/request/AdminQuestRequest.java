package online.lifeasgame.quest.api.admin.request;

import jakarta.validation.constraints.NotBlank;

public final class AdminQuestRequest {

    private AdminQuestRequest() {}

    public record Ensure(@NotBlank String code) {}

    public record Update(
            String targetType,
            Integer targetValue,
            String repeatRule,
            Integer rewardExp,
            java.util.Map<String, Integer> rewardStats,
            java.time.Instant dueAt
    ) {}
}
