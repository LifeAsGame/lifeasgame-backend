package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.domain.InstanceAttrs;
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
            int quantity,
            InstanceAttrs instanceAttrs,
            boolean bound
    ) {
        return playerInventory.add(
                itemCarryPolicy,
                quantity,
                instanceAttrs,
                bound
        );
    }
}
