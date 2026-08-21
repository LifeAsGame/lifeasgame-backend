package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.Currency;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.domain.event.EconomyEventType;
import online.lifeasgame.inventory.application.internal.InventoryMarketAvailabilityApi;
import online.lifeasgame.inventory.application.internal.InventoryMarketAvailabilityApi.EntrySnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ListingOpenService {

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final InventoryMarketAvailabilityApi inventoryMarketAvailabilityApi;
    private final ListingWriter listingWriter;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public EconomyResult.ListingId open(EconomyCommand.OpenListing command) {
        Long sellerPlayerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        EntrySnapshot entry = inventoryMarketAvailabilityApi.listWholeEntry(
                sellerPlayerId,
                command.inventoryEntryId()
        );
        Money totalPrice = Money.of(
                command.price(),
                Currency.parseOptional(command.currency(), Currency.GOLD)
        );
        Listing listing = listingWriter.create(Listing.open(
                sellerPlayerId,
                entry.inventoryEntryId(),
                entry.itemId(),
                entry.quantity(),
                totalPrice
        ));

        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.LISTING_OPENED)
                        .listingId(listing.getId())
                        .actorId(sellerPlayerId)
                        .occurredAt(Instant.now())
                        .build()
        );
        return new EconomyResult.ListingId(listing.getId());
    }
}
