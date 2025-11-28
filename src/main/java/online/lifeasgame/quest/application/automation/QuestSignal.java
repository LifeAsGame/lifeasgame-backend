package online.lifeasgame.quest.application.automation;

import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.quest.domain.QuestCode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestSignal {

    private final QuestCode questCode;
    private final Long playerId;
    private final QuestSignalType type;
    private final int progressDelta;
    private final Integer progressValue;
    private final Instant occurredAt;
    private final String correlationId;
    private final Map<String, Object> attributes;

    private QuestSignal(Builder builder) {
        this.questCode = Guard.notNull(builder.questCode, "questCode");
        this.playerId = Guard.notNull(builder.playerId, "playerId");
        this.type = Guard.notNull(builder.type, "type");
        this.progressDelta = builder.progressDelta;
        this.progressValue = builder.progressValue;
        this.occurredAt = builder.occurredAt == null ? Instant.now() : builder.occurredAt;
        this.correlationId = builder.correlationId;
        this.attributes = Map.copyOf(builder.attributes);
    }

    public QuestCode questCode() {
        return questCode;
    }

    public Long playerId() {
        return playerId;
    }

    public QuestSignalType type() {
        return type;
    }

    public int progressDelta() {
        return progressDelta;
    }

    public Integer progressValue() {
        return progressValue;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public String correlationId() {
        return correlationId;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public static Builder addProgress(QuestCode code, Long playerId, int delta) {
        Guard.minValue(delta, 1, "delta");
        return new Builder(code, playerId, QuestSignalType.ADD_PROGRESS).progressDelta(delta);
    }

    public static Builder setProgress(QuestCode code, Long playerId, int value) {
        Guard.minValue(value, 0, "progress");
        return new Builder(code, playerId, QuestSignalType.SET_PROGRESS).progressValue(value);
    }

    public boolean isSetOperation() {
        return type == QuestSignalType.SET_PROGRESS;
    }

    public static final class Builder {
        private final QuestCode questCode;
        private final Long playerId;
        private final QuestSignalType type;
        private int progressDelta;
        private Integer progressValue;
        private Instant occurredAt;
        private String correlationId;
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        private Builder(QuestCode questCode, Long playerId, QuestSignalType type) {
            this.questCode = questCode;
            this.playerId = playerId;
            this.type = type;
        }

        public Builder progressDelta(int progressDelta) {
            this.progressDelta = progressDelta;
            return this;
        }

        public Builder progressValue(int progressValue) {
            this.progressValue = progressValue;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key == null || key.isBlank() || value == null) {
                return this;
            }
            this.attributes.put(key, value);
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes.clear();
            if (attributes != null) {
                attributes.forEach(this::attribute);
            }
            return this;
        }

        public QuestSignal build() {
            return new QuestSignal(this);
        }
    }
}
