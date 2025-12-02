package online.lifeasgame.economy.domain.event;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.guard.Guard;

public record EconomyEvent(
        EconomyEventType type,
        Long actorId,
        Long listingId,
        Long tradeId,
        Long shopItemId,
        Long shopPurchaseId,
        String reservationToken,
        String correlationId,
        Instant occurredAt,
        Map<String, Object> attributes
) implements DomainEvent {

    public EconomyEvent {
        Guard.notNull(type, "type");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(attributes);
    }

    public String key() {
        if (listingId != null) {
            return "listing:" + listingId;
        }
        if (shopItemId != null) {
            return "shop:" + shopItemId;
        }
        if (shopPurchaseId != null) {
            return "shop-purchase:" + shopPurchaseId;
        }
        return type.name();
    }

    public static Builder builder(EconomyEventType type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final EconomyEventType type;
        private Long actorId;
        private Long listingId;
        private Long tradeId;
        private Long shopItemId;
        private Long shopPurchaseId;
        private String reservationToken;
        private String correlationId;
        private Instant occurredAt;
        private final Map<String, Object> attributes = new HashMap<>();

        private Builder(EconomyEventType type) {
            this.type = Guard.notNull(type, "type");
        }

        public Builder actorId(Long actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder listingId(Long listingId) {
            this.listingId = listingId;
            return this;
        }

        public Builder tradeId(Long tradeId) {
            this.tradeId = tradeId;
            return this;
        }

        public Builder shopItemId(Long shopItemId) {
            this.shopItemId = shopItemId;
            return this;
        }

        public Builder shopPurchaseId(Long shopPurchaseId) {
            this.shopPurchaseId = shopPurchaseId;
            return this;
        }

        public Builder reservationToken(String reservationToken) {
            this.reservationToken = reservationToken;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key != null && value != null) {
                attributes.put(key, value);
            }
            return this;
        }

        public EconomyEvent build() {
            return new EconomyEvent(
                    type,
                    actorId,
                    listingId,
                    tradeId,
                    shopItemId,
                    shopPurchaseId,
                    reservationToken,
                    correlationId,
                    occurredAt,
                    attributes
            );
        }
    }
}
