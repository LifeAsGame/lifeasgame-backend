package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.inventory.application.internal.InventoryMarketTransferApi;
import online.lifeasgame.inventory.domain.ItemCarryPolicy;
import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InventoryMarketTransferService
        implements InventoryMarketTransferApi {

    private final InventoryReader inventoryReader;
    private final ItemReader itemReader;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TransferResult transferWholeEntry(
            Long sellerPlayerId,
            Long buyerPlayerId,
            Long inventoryEntryId,
            Long expectedItemId,
            int expectedQuantity
    ) {
        if (sellerPlayerId == null
                || buyerPlayerId == null
                || inventoryEntryId == null
                || expectedItemId == null
                || expectedQuantity < 1
                || Objects.equals(sellerPlayerId, buyerPlayerId)) {
            throw new DomainException(
                    InventoryError.MARKET_TRANSFER_CONFLICT
            );
        }
        PlayerInventory first = inventoryReader
                .getByPlayerIdForUpdateOrThrow(
                        Math.min(sellerPlayerId, buyerPlayerId)
                );
        PlayerInventory second = inventoryReader
                .getByPlayerIdForUpdateOrThrow(
                        Math.max(sellerPlayerId, buyerPlayerId)
                );
        PlayerInventory seller = first.getPlayerId().equals(sellerPlayerId)
                ? first
                : second;
        PlayerInventory buyer = first.getPlayerId().equals(buyerPlayerId)
                ? first
                : second;

        PlayerInventory.MarketplaceTransfer transfer = seller
                .transferWholeMarketplaceEntryTo(
                        buyer,
                        inventoryEntryId,
                        expectedItemId,
                        expectedQuantity,
                        ItemCarryPolicy.from(
                                itemReader.getByIdOrThrow(expectedItemId)
                        )
                );
        domainEventPublisher.publishAll(buyer.pullEvents());
        return new TransferResult(
                transfer.sourceInventoryEntryId(),
                transfer.itemId(),
                transfer.quantity()
        );
    }
}
