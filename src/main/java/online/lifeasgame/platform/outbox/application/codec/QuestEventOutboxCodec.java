package online.lifeasgame.platform.outbox.application.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;

import java.time.Instant;
import java.util.Map;

final class QuestEventOutboxCodec implements OutboxEventCodec<QuestEvent> {

    static final String ALIAS = "quest.event.v1";

    private final ObjectMapper objectMapper;

    QuestEventOutboxCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String alias() {
        return ALIAS;
    }

    @Override
    public Class<QuestEvent> eventType() {
        return QuestEvent.class;
    }

    @Override
    public String encode(QuestEvent event) {
        Payload payload = new Payload(
                event.type(),
                event.playerId(),
                event.questId(),
                event.questCode(),
                OutboxAttributeValues.encode(event.attributes()),
                event.occurredAt(),
                event.correlationId()
        );
        return write(payload);
    }

    @Override
    public QuestEvent decode(String payload) {
        Payload decoded = read(payload, Payload.class);
        return new QuestEvent(
                decoded.type(),
                decoded.playerId(),
                decoded.questId(),
                decoded.questCode(),
                OutboxAttributeValues.decode(decoded.attributes()),
                decoded.occurredAt(),
                decoded.correlationId()
        );
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw codecFailure(exception);
        }
    }

    private <T> T read(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException exception) {
            throw codecFailure(exception);
        }
    }

    private DomainException codecFailure(JsonProcessingException exception) {
        return new DomainException(
                OutboxError.OUTBOX_EVENT_CODEC_FAILED,
                null,
                exception
        );
    }

    private record Payload(
            QuestEventType type,
            Long playerId,
            Long questId,
            String questCode,
            Map<String, OutboxAttributeValues.Value> attributes,
            Instant occurredAt,
            String correlationId
    ) {
    }
}
