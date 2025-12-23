package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.query.InventoryStackQuery;
import online.lifeasgame.inventory.application.query.MailboxStackQuery;
import online.lifeasgame.inventory.domain.error.ItemError;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemStackPolicyChecker {

    private final InventoryStackQuery inventoryStackQuery;
    private final MailboxStackQuery mailboxStackQuery;

    public void assertNoPolicyConflict(Long itemId, int limit) {
        if (inventoryStackQuery.countStacksExceeding(itemId, limit) > 0) {
            throw new DomainException(ItemError.POLICY_CONFLICT);
        }
        if (mailboxStackQuery.countStacksExceeding(itemId, limit) > 0) {
            throw new DomainException(ItemError.POLICY_CONFLICT);
        }
    }
}
