package online.lifeasgame.quest.application.internal.event;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.quest.domain.event.QuestEvent;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record QuestRewardReadyFact(
        int eventVersion,
        Long playerId,
        Long acceptanceId,
        String rewardProfileCode,
        Long questId,
        String questCode,
        int questDefinitionVersion,
        Instant occurredAt,
        String correlationId
) implements DomainEvent {

    public static final int EVENT_VERSION = 1;
    private static final Set<String> FINAL_MARKERS = Set.of(
            "questSemanticCategory",
            "progressSource",
            "repeatPolicy"
    );

    public QuestRewardReadyFact {
        require(eventVersion == EVENT_VERSION, "unsupported eventVersion");
        require(playerId != null && playerId > 0, "playerId must be positive");
        require(
                acceptanceId != null && acceptanceId > 0,
                "acceptanceId must be positive"
        );
        require(
                rewardProfileCode != null
                        && !rewardProfileCode.isBlank(),
                "rewardProfileCode must not be blank"
        );
        rewardProfileCode = rewardProfileCode.strip();
        require(questId != null && questId > 0, "questId must be positive");
        require(
                questCode != null && !questCode.isBlank(),
                "questCode must not be blank"
        );
        questCode = questCode.strip();
        require(
                questDefinitionVersion > 0,
                "questDefinitionVersion must be positive"
        );
        require(occurredAt != null, "occurredAt must not be null");
        require(
                correlationId != null && !correlationId.isBlank(),
                "correlationId must not be blank"
        );
    }

    public static Optional<QuestRewardReadyFact> from(
            QuestEvent event,
            Instant occurredAt,
            String correlationId
    ) {
        Map<String, Object> attributes = event.attributes();
        Object profile = attributes.get("rewardProfileCode");
        if (profile == null) {
            if (attributes.keySet().stream().anyMatch(FINAL_MARKERS::contains)) {
                throw new IllegalArgumentException(
                        "rewardProfileCode is required for final quest contract"
                );
            }
            return Optional.empty();
        }
        if (!(profile instanceof String profileCode)) {
            throw new IllegalArgumentException(
                    "rewardProfileCode must be a string"
            );
        }
        return Optional.of(new QuestRewardReadyFact(
                EVENT_VERSION,
                event.playerId(),
                positiveLong(attributes.get("acceptanceId"), "acceptanceId"),
                profileCode,
                event.questId(),
                event.questCode(),
                positiveInt(
                        attributes.get("questDefinitionVersion"),
                        "questDefinitionVersion"
                ),
                occurredAt,
                correlationId
        ));
    }

    private static Long positiveLong(Object value, String name) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(name + " must be numeric");
        }
        try {
            long result = switch (number) {
                case Byte item -> item.longValue();
                case Short item -> item.longValue();
                case Integer item -> item.longValue();
                case Long item -> item;
                case BigInteger item -> item.longValueExact();
                default -> throw new IllegalArgumentException(
                        name + " must be an integer"
                );
            };
            require(result > 0, name + " must be positive");
            return result;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " is out of range", exception);
        }
    }

    private static int positiveInt(Object value, String name) {
        try {
            return Math.toIntExact(positiveLong(value, name));
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    name + " is out of range",
                    exception
            );
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
