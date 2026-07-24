package online.lifeasgame.platform.outbox.application.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;

final class JacksonOutboxEventCodec<T extends DomainEvent>
        implements OutboxEventCodec<T> {

    private final String alias;
    private final Class<T> eventType;
    private final ObjectMapper objectMapper;

    JacksonOutboxEventCodec(
            String alias,
            Class<T> eventType,
            ObjectMapper objectMapper
    ) {
        this.alias = alias;
        this.eventType = eventType;
        this.objectMapper = objectMapper;
    }

    @Override
    public String alias() {
        return alias;
    }

    @Override
    public Class<T> eventType() {
        return eventType;
    }

    @Override
    public String encode(T event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new DomainException(
                    OutboxError.OUTBOX_EVENT_CODEC_FAILED,
                    null,
                    exception
            );
        }
    }

    @Override
    public T decode(String payload) {
        try {
            return objectMapper.readValue(payload, eventType);
        } catch (JsonProcessingException exception) {
            throw new DomainException(
                    OutboxError.OUTBOX_EVENT_CODEC_FAILED,
                    null,
                    exception
            );
        }
    }
}
