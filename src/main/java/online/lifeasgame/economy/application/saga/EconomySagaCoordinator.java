package online.lifeasgame.economy.application.saga;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.domain.event.EconomyEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EconomySagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(EconomySagaCoordinator.class);

    private final DomainEventPublisher domainEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEconomyEvent(EconomyEvent event) {
        if (event.type() == EconomyEventType.FULFILLMENT_READY) {
            return;
        }

        if (event.type() == EconomyEventType.LISTING_PURCHASED || event.type() == EconomyEventType.SHOP_PURCHASE_COMPLETED) {
            String correlation = event.correlationId() == null ?
                    event.key() + ":fulfillment" : event.correlationId() + ":fulfillment";
            log.debug("Chaining fulfillment stage for economy event {}", event.type());

            domainEventPublisher.publish(
                    EconomyEvent.builder(EconomyEventType.FULFILLMENT_READY)
                            .actorId(event.actorId())
                            .listingId(event.listingId())
                            .shopItemId(event.shopItemId())
                            .shopPurchaseId(event.shopPurchaseId())
                            .correlationId(correlation)
                            .occurredAt(Instant.now())
                            .build()
            );
        }
    }
}
