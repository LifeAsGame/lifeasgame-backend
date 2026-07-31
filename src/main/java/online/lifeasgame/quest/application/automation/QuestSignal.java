package online.lifeasgame.quest.application.automation;

import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.error.QuestError;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestSignal {

    public static final int MAX_CORRELATION_ID_LENGTH = 120;

    private final QuestCode questCode;
    private final Long playerId;
    private final QuestSignalType type;
    private final int progressDelta;
    private final Integer progressValue;
    private final Instant occurredAt;
    private final String correlationId;
    private final QuestSignalAcceptancePolicy acceptancePolicy;
    private final String periodKey;
    private final Map<String, Object> attributes;

    private QuestSignal(Builder builder) {
        this.questCode = Guard.notNull(builder.questCode, "questCode");
        this.playerId = Guard.notNull(builder.playerId, "playerId");
        this.type = Guard.notNull(builder.type, "type");
        this.progressDelta = builder.progressDelta;
        this.progressValue = builder.progressValue;
        this.occurredAt = builder.occurredAt == null ? Instant.now() : builder.occurredAt;
        this.correlationId = normalizeCorrelation(builder.correlationId);
        this.acceptancePolicy = builder.acceptancePolicy;
        this.periodKey = builder.periodKey;
        this.attributes = Collections.unmodifiableMap(
                new LinkedHashMap<>(builder.attributes)
        );
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

    public QuestSignalAcceptancePolicy acceptancePolicy() {
        return acceptancePolicy;
    }

    public String periodKey() {
        return periodKey;
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

    private String normalizeCorrelation(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(QuestError.QUEST_SIGNAL_CORRELATION_REQUIRED);
        }
        String normalized = raw.trim();
        if (normalized.length() > MAX_CORRELATION_ID_LENGTH) {
            throw new DomainException(QuestError.QUEST_SIGNAL_CORRELATION_TOO_LONG);
        }
        return normalized;
    }

    public static final class Builder {
        private final QuestCode questCode;
        private final Long playerId;
        private final QuestSignalType type;
        private int progressDelta;
        private Integer progressValue;
        private Instant occurredAt;
        private String correlationId;
        private QuestSignalAcceptancePolicy acceptancePolicy =
                QuestSignalAcceptancePolicy.AUTO_CREATE;
        private String periodKey;
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

        public Builder acceptancePolicy(
                QuestSignalAcceptancePolicy acceptancePolicy
        ) {
            this.acceptancePolicy = Guard.notNull(
                    acceptancePolicy,
                    "acceptancePolicy"
            );
            return this;
        }

        public Builder periodKey(String periodKey) {
            this.periodKey = periodKey;
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key == null || key.isBlank() || value == null) {
                return this;
            }
            this.attributes.put(key, value);
            return this;
        }

        public Builder nullableAttribute(String key, Object value) {
            if (key == null || key.isBlank()) {
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
