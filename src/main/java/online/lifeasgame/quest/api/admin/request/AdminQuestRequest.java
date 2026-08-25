package online.lifeasgame.quest.api.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
            Instant dueAt,
            String semanticCategory,
            String progressSource,
            String repeatPolicy,
            @Size(max = 80)
            @Pattern(regexp = ".*\\S.*")
            String roleTemplateCode
    ) {
        public Update(
                Integer definitionVersion,
                String targetType,
                Integer targetValue,
                String repeatRule,
                String rewardProfileCode,
                Integer rewardExp,
                Map<String, Integer> rewardStats,
                Instant dueAt
        ) {
            this(
                    definitionVersion,
                    targetType,
                    targetValue,
                    repeatRule,
                    rewardProfileCode,
                    rewardExp,
                    rewardStats,
                    dueAt,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    public record AdjustProgress(
            @NotNull @PositiveOrZero Integer delta,
            @NotBlank
            @Size(max = 512)
            @Pattern(regexp = "(?=.*[^\\p{Cf}\\p{Zs}])[^\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]*")
            String reason
    ) {
    }

    public record ChangeStatus(
            @NotBlank String status,
            @NotBlank
            @Size(max = 512)
            @Pattern(regexp = "(?=.*[^\\p{Cf}\\p{Zs}])[^\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]*")
            String reason
    ) {
    }

    public record TriggerReward(
            @Size(max = 120) String idempotencyKey,
            @Size(max = 120) String correlationId
    ) {
    }
}
