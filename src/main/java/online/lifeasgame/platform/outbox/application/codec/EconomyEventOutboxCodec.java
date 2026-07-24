package online.lifeasgame.platform.outbox.application.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.domain.event.EconomyEventType;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;

import java.time.Instant;
import java.util.Map;

final class EconomyEventOutboxCodec
        implements OutboxEventCodec<EconomyEvent> {

    static final String ALIAS = "economy.event.v1";

    private final ObjectMapper objectMapper;

    EconomyEventOutboxCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String alias() {
        return ALIAS;
    }

    @Override
    public Class<EconomyEvent> eventType() {
        return EconomyEvent.class;
    }

    @Override
    public String encode(EconomyEvent event) {
        Payload payload = new Payload(
                event.type(),
                event.actorId(),
                event.listingId(),
                event.tradeId(),
                event.shopItemId(),
                event.shopPurchaseId(),
                event.reservationToken(),
                event.correlationId(),
                event.occurredAt(),
                OutboxAttributeValues.encode(event.attributes())
        );
        return write(payload);
    }

    @Override
    public EconomyEvent decode(String payload) {
        Payload decoded = read(payload, Payload.class);
        return new EconomyEvent(
                decoded.type(),
                decoded.actorId(),
                decoded.listingId(),
                decoded.tradeId(),
                decoded.shopItemId(),
                decoded.shopPurchaseId(),
                decoded.reservationToken(),
                decoded.correlationId(),
                decoded.occurredAt(),
                OutboxAttributeValues.decode(decoded.attributes())
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
            EconomyEventType type,
            Long actorId,
            Long listingId,
            Long tradeId,
            Long shopItemId,
            Long shopPurchaseId,
            String reservationToken,
            String correlationId,
            Instant occurredAt,
            Map<String, OutboxAttributeValues.Value> attributes
    ) {
    }
}
