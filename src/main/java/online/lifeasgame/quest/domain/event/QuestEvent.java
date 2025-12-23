package online.lifeasgame.quest.domain.event;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.quest.domain.Quest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public record QuestEvent(
        QuestEventType type,
        Long playerId,
        Long questId,
        String questCode,
        Map<String, Object> attributes,
        Instant occurredAt,
        String correlationId
) implements DomainEvent {

    public QuestEvent {
        Guard.notNull(type, "type");
        Guard.notNull(occurredAt, "occurredAt");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static Builder builder(QuestEventType type) {
        return new Builder(type);
    }

    public String key() {
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        if (playerId != null) {
            return "player:" + playerId;
        }
        if (questCode != null && !questCode.isBlank()) {
            return "quest:" + questCode;
        }
        if (questId != null) {
            return "questId:" + questId;
        }
        return type.name();
    }

    public static QuestEvent snapshot(QuestEventType type, Quest quest, String correlationId) {
        return QuestEvent.builder(type)
                .questId(quest.getId())
                .questCode(quest.getCode())
                .attribute("title", quest.getTitle().value())
                .attribute("category", quest.getCategory().name())
                .attribute("targetType", quest.target().type().name())
                .attribute("targetValue", quest.target().value())
                .attribute("repeatRule", quest.getRepeatRule().name())
                .attribute("rewardExp", quest.getReward().exp())
                .attribute("rewardStats", quest.getReward().stats().stats())
                .attribute("dueAt", quest.getDueAt())
                .occurredAt(Instant.now())
                .correlationId(correlationId)
                .build();
    }

    public static final class Builder {
        private final QuestEventType type;
        private Long playerId;
        private Long questId;
        private String questCode;
        private final Map<String, Object> attributes = new LinkedHashMap<>();
        private Instant occurredAt = Instant.now();
        private String correlationId;

        private Builder(QuestEventType type) {
            this.type = Guard.notNull(type, "type");
        }

        public Builder playerId(Long playerId) {
            this.playerId = playerId;
            return this;
        }

        public Builder questId(Long questId) {
            this.questId = questId;
            return this;
        }

        public Builder questCode(String questCode) {
            this.questCode = questCode;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes.clear();
            if (attributes != null) {
                attributes.forEach(this::attribute);
            }
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key == null || key.isBlank()) {
                return this;
            }
            if (value == null) {
                this.attributes.remove(key);
            } else {
                this.attributes.put(key, value);
            }
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = Guard.notNull(occurredAt, "occurredAt");
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public QuestEvent build() {
            Map<String, Object> sanitized = attributes.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null)
                    .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));
            return new QuestEvent(type, playerId, questId, questCode, sanitized, occurredAt, correlationId);
        }
    }
}
