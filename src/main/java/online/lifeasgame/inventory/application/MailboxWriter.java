package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.model.MailboxSpec;
import online.lifeasgame.inventory.domain.ItemCarryPolicy;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import online.lifeasgame.inventory.domain.SlotIndex;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class MailboxWriter {

    public SlotIndex deliver(PlayerMailbox playerMailbox, ItemCarryPolicy policy, MailboxSpec.Deliver cmd) {
        return playerMailbox.deliver(policy, cmd.quantity(), cmd.instanceAttrs(), cmd.bound());
    }

    public PlayerMailbox.ClaimedSlice claimSlice(PlayerMailbox playerMailbox, ItemCarryPolicy policy, MailboxSpec.Claim cmd) {
        return playerMailbox.claim(SlotIndex.of(cmd.slotIndex()), cmd.quantity(), policy);
    }
}
