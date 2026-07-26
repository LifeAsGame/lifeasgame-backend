package online.lifeasgame.quest.api.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public final class AdminQuestRequest {

    private AdminQuestRequest() {}

    public record Ensure(@NotBlank String code) {}

    public record Update(
            @Positive Integer definitionVersion,
            String targetType,
            Integer targetValue,
            String repeatRule,
            @Size(max = 80) String rewardProfileCode,
            Integer rewardExp,
            Map<String, Integer> rewardStats,
            Instant dueAt
    ) {}

    public record AdjustProgress(
            @NotBlank String type,
            Integer delta,
            Integer value,
            @Size(max = 120) String idempotencyKey
    ) {
    }

    public record ChangeStatus(
            @NotBlank String status,
            @Size(max = 200) String reason,
            @Size(max = 120) String idempotencyKey
    ) {
    }

    public record TriggerReward(
            @Size(max = 120) String idempotencyKey,
            @Size(max = 120) String correlationId
    ) {
    }
}
