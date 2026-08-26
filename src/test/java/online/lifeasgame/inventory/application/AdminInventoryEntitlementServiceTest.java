package online.lifeasgame.inventory.application;

import online.lifeasgame.adminaudit.application.internal.AdminAuditInternalApi;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.inventory.application.command.AdminInventoryEntitlementCommand;
import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.application.result.MailboxResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Inventory entitlement application contract")
class AdminInventoryEntitlementServiceTest {

    private static final long PLAYER_ID = 310L;
    private static final long ITEM_ID = 3100L;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private MailboxService mailboxService;

    @Mock
    private AdminAuditInternalApi adminAuditApi;

    @Nested
    @DisplayName("Admin entitlement command metadata를 만들 때")
    class CommandValidation {

        @Test
        @DisplayName("positive identity/quantity와 safe metadata만 허용한다")
        void rejectsUnsafeMetadata() {
            assertThatThrownBy(() -> inventory(
                    0L, ITEM_ID, 1, "CASE-310", "key-310", null
            )).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> mailbox(
                    PLAYER_ID, -1L, 1, "CASE-310", "key-310", null
            )).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> inventory(
                    PLAYER_ID, ITEM_ID, 0, "CASE-310", "key-310", null
            )).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> mailbox(
                    PLAYER_ID,
                    ITEM_ID,
                    1,
                    "CASE-310\u202Eprivate",
                    "key-310",
                    null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("single-line");
            assertThatThrownBy(() -> inventory(
                    PLAYER_ID, ITEM_ID, 1, "CASE-310", "unsafe key", null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe format");
            assertThatThrownBy(() -> mailbox(
                    PLAYER_ID, ITEM_ID, 1, "CASE-310", "key-310", "unsafe id"
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unsafe format");
        }

        @Test
        @DisplayName("correlation이 없으면 safe UUID를 생성하고 reason을 정규화한다")
        void generatesCorrelation() {
            assertThat(inventory(
                    PLAYER_ID,
                    ITEM_ID,
                    1,
                    " CASE-310 ",
                    "key-310",
                    null
            )).satisfies(command -> {
                assertThat(command.reason()).isEqualTo("CASE-310");
                assertThat(command.correlationId()).matches("[a-f0-9-]{36}");
            });
        }
    }

    @Test
    @DisplayName("Inventory는 canonical empty attrs로 지급한 뒤 success Audit을 append한다")
    void addsInventoryThenAudits() {
        InventoryResult.Slots expected = new InventoryResult.Slots(List.of(0));
        when(inventoryService.add(any(), any())).thenReturn(expected);
        AdminInventoryEntitlementService service = service();

        InventoryResult.Slots result = service.addToInventory(inventory(
                PLAYER_ID,
                ITEM_ID,
                2,
                "CASE-310-INVENTORY",
                "inventory-310",
                "request-inventory"
        ));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<InventoryCommand.Add> entitlement =
                ArgumentCaptor.forClass(InventoryCommand.Add.class);
        ArgumentCaptor<AdminAuditInternalApi.AppendCommand> audit =
                ArgumentCaptor.forClass(AdminAuditInternalApi.AppendCommand.class);
        InOrder order = inOrder(inventoryService, adminAuditApi);
        order.verify(inventoryService).add(
                org.mockito.ArgumentMatchers.eq(PLAYER_ID),
                entitlement.capture()
        );
        order.verify(adminAuditApi).append(audit.capture());
        assertThat(entitlement.getValue()).satisfies(command -> {
            assertThat(command.itemId()).isEqualTo(ITEM_ID);
            assertThat(command.quantity()).isEqualTo(2);
            assertThat(command.instanceAttrs()).isEmpty();
            assertThat(command.bound()).isFalse();
        });
        assertAudit(
                audit.getValue(),
                "INVENTORY_ITEM_ADD",
                "PLAYER_INVENTORY",
                "CASE-310-INVENTORY",
                "inventory-310",
                "request-inventory"
        );
    }

    @Test
    @DisplayName("Mailbox는 canonical empty attrs로 지급한 뒤 success Audit을 append한다")
    void deliversMailboxThenAudits() {
        MailboxResult.Slot expected = new MailboxResult.Slot(0);
        when(mailboxService.deliver(any(), any())).thenReturn(expected);
        AdminInventoryEntitlementService service = service();

        MailboxResult.Slot result = service.deliverToMailbox(mailbox(
                PLAYER_ID,
                ITEM_ID,
                1,
                "CASE-310-MAILBOX",
                "mailbox-310",
                "request-mailbox"
        ));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<MailboxCommand.Deliver> entitlement =
                ArgumentCaptor.forClass(MailboxCommand.Deliver.class);
        ArgumentCaptor<AdminAuditInternalApi.AppendCommand> audit =
                ArgumentCaptor.forClass(AdminAuditInternalApi.AppendCommand.class);
        InOrder order = inOrder(mailboxService, adminAuditApi);
        order.verify(mailboxService).deliver(
                org.mockito.ArgumentMatchers.eq(PLAYER_ID),
                entitlement.capture()
        );
        order.verify(adminAuditApi).append(audit.capture());
        assertThat(entitlement.getValue()).satisfies(command -> {
            assertThat(command.itemId()).isEqualTo(ITEM_ID);
            assertThat(command.quantity()).isEqualTo(1);
            assertThat(command.instanceAttrs()).isEmpty();
            assertThat(command.bound()).isFalse();
        });
        assertAudit(
                audit.getValue(),
                "MAILBOX_ITEM_DELIVERY",
                "PLAYER_MAILBOX",
                "CASE-310-MAILBOX",
                "mailbox-310",
                "request-mailbox"
        );
    }

    private AdminInventoryEntitlementService service() {
        return new AdminInventoryEntitlementService(
                inventoryService,
                mailboxService,
                adminAuditApi
        );
    }

    private AdminInventoryEntitlementCommand.AddToInventory inventory(
            Long playerId,
            Long itemId,
            int quantity,
            String reason,
            String key,
            String correlationId
    ) {
        return new AdminInventoryEntitlementCommand.AddToInventory(
                playerId,
                itemId,
                quantity,
                false,
                reason,
                key,
                correlationId
        );
    }

    private AdminInventoryEntitlementCommand.DeliverToMailbox mailbox(
            Long playerId,
            Long itemId,
            int quantity,
            String reason,
            String key,
            String correlationId
    ) {
        return new AdminInventoryEntitlementCommand.DeliverToMailbox(
                playerId,
                itemId,
                quantity,
                false,
                reason,
                key,
                correlationId
        );
    }

    private void assertAudit(
            AdminAuditInternalApi.AppendCommand audit,
            String action,
            String targetType,
            String reason,
            String key,
            String correlationId
    ) {
        assertThat(audit.action().value()).isEqualTo(action);
        assertThat(audit.targetType().value()).isEqualTo(targetType);
        assertThat(audit.targetId()).isEqualTo(Long.toString(PLAYER_ID));
        assertThat(audit.reason()).isEqualTo(reason);
        assertThat(audit.result()).isEqualTo(AdminAuditResult.SUCCESS);
        assertThat(audit.idempotencyKey()).isEqualTo(key);
        assertThat(audit.correlationId()).isEqualTo(correlationId);
    }
}
