package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentReadApi;
import online.lifeasgame.inventory.application.query.InventoryEntryView;
import online.lifeasgame.inventory.application.query.InventoryQuery;
import online.lifeasgame.inventory.application.query.OwnedEquipmentItemView;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryService implements InventoryEquipmentReadApi {

    private final InventoryQuery inventoryQuery;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public InventoryResult.Entries list() {
        return list(currentPlayerAccessor.currentPlayerIdOrThrow());
    }

    public InventoryResult.Entries list(Long playerId) {
        List<InventoryEntryView> entryViews =
                inventoryQuery.findInventoryEntries(playerId);
        return InventoryResult.Entries.fromViews(entryViews);
    }

    public InventoryResult.Entry getEntry(Long itemInstanceId) {
        return getEntry(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                itemInstanceId
        );
    }

    public InventoryResult.Entry getEntry(
            Long playerId,
            Long itemInstanceId
    ) {
        InventoryEntryView entryView = inventoryQuery
                .findInventoryEntryByInstanceId(playerId, itemInstanceId)
                .orElseThrow(() -> new DomainException(
                        InventoryError.INVENTORY_ENTRY_NOT_FOUND
                ));
        return InventoryResult.Entry.fromView(entryView);
    }

    @Override
    public OwnedEquipmentItem getOwnedItem(
            Long playerId,
            Long itemInstanceId
    ) {
        OwnedEquipmentItemView item = inventoryQuery
                .findOwnedEquipmentItem(playerId, itemInstanceId)
                .orElseThrow(() -> new DomainException(
                        InventoryError.INVENTORY_ENTRY_NOT_FOUND
                ));
        return new OwnedEquipmentItem(
                item.itemInstanceId(),
                item.itemId(),
                item.category().name(),
                item.type().name(),
                item.equipmentCompatibilityKind() == null
                        ? null
                        : item.equipmentCompatibilityKind().name()
        );
    }
}
