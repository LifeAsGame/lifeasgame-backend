package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.model.InventorySpec;
import online.lifeasgame.inventory.domain.ItemCarryPolicy;
import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.SlotIndex;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class InventoryWriter {

    public List<SlotIndex> add(
            PlayerInventory playerInventory,
            ItemCarryPolicy itemCarryPolicy,
            InventorySpec.Add spec
    ) {
        return playerInventory.add(
                itemCarryPolicy,
                spec.quantity(),
                spec.instanceAttrs(),
                spec.bound()
        );
    }

    public void remove(PlayerInventory playerInventory, SlotIndex slotIndex, int quantity) {
        playerInventory.remove(
                slotIndex,
                quantity
        );
    }

    public void move(PlayerInventory playerInventory, SlotIndex from, SlotIndex to) {
        playerInventory.moveWithin(from, to);
    }

    public void merge(
            PlayerInventory playerInventory,
            ItemCarryPolicy itemCarryPolicy,
            SlotIndex from,
            SlotIndex to
    ) {
        playerInventory.merge(from, to, itemCarryPolicy);
    }

    public SlotIndex split(
            PlayerInventory playerInventory,
            ItemCarryPolicy itemCarryPolicy,
            SlotIndex from,
            SlotIndex to,
            int quantity
    ) {
        return playerInventory.split(from, to, quantity, itemCarryPolicy);
    }
}
