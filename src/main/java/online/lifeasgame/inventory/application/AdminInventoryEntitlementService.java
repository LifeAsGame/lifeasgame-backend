package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;
import online.lifeasgame.inventory.application.command.AdminInventoryEntitlementCommand;
import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.application.result.MailboxResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminInventoryEntitlementService {

    private static final AdminAuditAction INVENTORY_ACTION =
            new AdminAuditAction("INVENTORY_ITEM_ADD");
    private static final AdminAuditAction MAILBOX_ACTION =
            new AdminAuditAction("MAILBOX_ITEM_DELIVERY");
    private static final AdminAuditTargetType INVENTORY_TARGET =
            new AdminAuditTargetType("PLAYER_INVENTORY");
    private static final AdminAuditTargetType MAILBOX_TARGET =
            new AdminAuditTargetType("PLAYER_MAILBOX");

    private final InventoryService inventoryService;
    private final MailboxService mailboxService;
    private final AdminAuditInternalApi adminAuditApi;

    @Transactional
    public InventoryResult.Slots addToInventory(
            AdminInventoryEntitlementCommand.AddToInventory command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        InventoryResult.Slots result = inventoryService.add(
                command.playerId(),
                new InventoryCommand.Add(
                        command.itemId(),
                        command.quantity(),
                        Map.of(),
                        command.bound()
                )
        );
        appendAudit(
                INVENTORY_ACTION,
                INVENTORY_TARGET,
                command.playerId(),
                command.reason(),
                command.correlationId(),
                command.idempotencyKey()
        );
        return result;
    }

    @Transactional
    public MailboxResult.Slot deliverToMailbox(
            AdminInventoryEntitlementCommand.DeliverToMailbox command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        MailboxResult.Slot result = mailboxService.deliver(
                command.playerId(),
                new MailboxCommand.Deliver(
                        command.itemId(),
                        command.quantity(),
                        Map.of(),
                        command.bound()
                )
        );
        appendAudit(
                MAILBOX_ACTION,
                MAILBOX_TARGET,
                command.playerId(),
                command.reason(),
                command.correlationId(),
                command.idempotencyKey()
        );
        return result;
    }

    private void appendAudit(
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            Long playerId,
            String reason,
            String correlationId,
            String idempotencyKey
    ) {
        adminAuditApi.append(new AdminAuditInternalApi.AppendCommand(
                action,
                targetType,
                playerId.toString(),
                reason,
                AdminAuditResult.SUCCESS,
                correlationId,
                idempotencyKey
        ));
    }
}
