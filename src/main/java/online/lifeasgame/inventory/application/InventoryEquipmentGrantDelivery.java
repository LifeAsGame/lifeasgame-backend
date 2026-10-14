package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentGrantApi;
import online.lifeasgame.inventory.domain.InstanceAttrs;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCarryPolicy;
import online.lifeasgame.inventory.domain.ItemCode;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.inventory.domain.repository.PlayerMailboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryEquipmentGrantDelivery implements InventoryEquipmentGrantApi {
    private final InventoryContainerProvisioningService provisioning;
    private final PlayerMailboxRepository mailboxes;
    private final ItemReader items;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deliverEquipment(Long playerId, String itemCode) {
        Item item = items.getByCodeOrThrow(ItemCode.of(itemCode));
        provisioning.ensureContainers(playerId);
        PlayerMailbox mailbox = mailboxes.findByPlayerIdForUpdate(playerId)
                .orElseThrow(() -> new DomainException(InventoryError.CONTAINER_NOT_FOUND));
        mailbox.deliver(ItemCarryPolicy.from(item), 1, InstanceAttrs.empty(), true);
    }
}
