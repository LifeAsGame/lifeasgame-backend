package online.lifeasgame.quest.api.admin.request;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

public final class AdminQuestRequest {

    private AdminQuestRequest() {}

    public record Ensure(@NotBlank String code) {}

    public record Update(
            String targetType,
            Integer targetValue,
            String repeatRule,
            Integer rewardExp,
            Map<String, Integer> rewardStats,
            Instant dueAt
    ) {}
}
