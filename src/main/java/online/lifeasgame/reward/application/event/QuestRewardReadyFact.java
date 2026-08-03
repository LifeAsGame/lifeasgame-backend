package online.lifeasgame.reward.application.event;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.reward.domain.error.RewardError;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record QuestRewardReadyFact(
        Long playerId,
        Long acceptanceId,
        String rewardProfileCode,
        Long questId,
        String questCode,
        Integer questDefinitionVersion,
        Instant occurredAt,
        String correlationId
) {
    private static final Set<String> FINAL_MARKERS = Set.of(
            "questSemanticCategory",
            "progressSource",
            "repeatPolicy"
    );

    public static Optional<QuestRewardReadyFact> from(QuestEvent event) {
        Map<String, Object> attributes = event.attributes();
        if (!attributes.containsKey("rewardProfileCode")) {
            if (attributes.keySet().stream().anyMatch(FINAL_MARKERS::contains)) {
                throw invalid(RewardError.REWARD_PROFILE_CODE_REQUIRED);
            }
            return Optional.empty();
        }

        String rewardProfileCode = profileCode(
                attributes.get("rewardProfileCode")
        );
        Long playerId = positive(
                event.playerId(),
                RewardError.REWARD_SETTLEMENT_PLAYER_ID_REQUIRED
        );
        Long acceptanceId = positiveNumber(
                attributes.get("acceptanceId"),
                RewardError.REWARD_SETTLEMENT_SOURCE_ID_REQUIRED
        );
        Integer definitionVersion =
                attributes.get("questDefinitionVersion") instanceof Number value
                        ? value.intValue()
                        : null;
        return Optional.of(new QuestRewardReadyFact(
                playerId,
                acceptanceId,
                rewardProfileCode,
                event.questId(),
                event.questCode(),
                definitionVersion,
                event.occurredAt(),
                event.correlationId()
        ));
    }

    private static String profileCode(Object value) {
        if (!(value instanceof String code)) {
            throw invalid(RewardError.REWARD_PROFILE_CODE_REQUIRED);
        }
        String normalized = code.strip();
        if (normalized.isBlank()) {
            throw invalid(RewardError.REWARD_PROFILE_CODE_REQUIRED);
        }
        return normalized;
    }

    private static Long positive(Long value, RewardError error) {
        if (value == null || value <= 0) {
            throw invalid(error);
        }
        return value;
    }

    private static Long positiveNumber(Object value, RewardError error) {
        if (!(value instanceof Number number)) {
            throw invalid(error);
        }
        try {
            long result = switch (number) {
                case Byte item -> item.longValue();
                case Short item -> item.longValue();
                case Integer item -> item.longValue();
                case Long item -> item;
                case BigInteger item -> item.longValueExact();
                default -> throw invalid(error);
            };
            if (result <= 0) {
                throw invalid(error);
            }
            return result;
        } catch (ArithmeticException exception) {
            throw new DomainException(error, null, exception);
        }
    }

    private static DomainException invalid(RewardError error) {
        return new DomainException(error);
    }
}
