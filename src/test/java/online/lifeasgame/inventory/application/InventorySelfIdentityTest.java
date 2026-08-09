package online.lifeasgame.inventory.application;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.query.InventoryQuery;
import online.lifeasgame.inventory.application.query.MailboxQuery;
import online.lifeasgame.inventory.application.result.InventoryResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InventorySelfIdentityTest {

    private static final Long PLAYER_ID = 247L;

    @Test
    void inventoryCommandAndQueryResolveCurrentPlayer() {
        CurrentPlayerAccessor accessor = mock(CurrentPlayerAccessor.class);
        when(accessor.currentPlayerIdOrThrow()).thenReturn(PLAYER_ID);
        InventoryService service = spy(new InventoryService(
                mock(InventoryReader.class),
                mock(ItemReader.class),
                mock(DomainEventPublisher.class),
                accessor
        ));
        InventoryCommand.Remove remove = new InventoryCommand.Remove(1, 1);
        doNothing().when(service).remove(PLAYER_ID, remove);
        InventoryQuery query = mock(InventoryQuery.class);
        when(query.findInventoryEntries(PLAYER_ID)).thenReturn(List.of());
        InventoryQueryService queryService =
                new InventoryQueryService(query, accessor);

        service.remove(remove);
        InventoryResult.Entries entries = queryService.list();

        verify(service).remove(PLAYER_ID, remove);
        verify(query).findInventoryEntries(PLAYER_ID);
        assertThat(entries.entryViews()).isEmpty();
    }

    @Test
    void mailboxCommandAndQueryResolveCurrentPlayer() {
        CurrentPlayerAccessor accessor = mock(CurrentPlayerAccessor.class);
        when(accessor.currentPlayerIdOrThrow()).thenReturn(PLAYER_ID);
        MailboxService service = spy(new MailboxService(
                mock(MailboxReader.class),
                mock(InventoryReader.class),
                mock(ItemReader.class),
                accessor
        ));
        MailboxCommand.Delete delete = new MailboxCommand.Delete(1);
        doNothing().when(service).delete(PLAYER_ID, delete);
        MailboxQuery query = mock(MailboxQuery.class);
        when(query.findMailboxEntries(PLAYER_ID)).thenReturn(List.of());
        MailboxQueryService queryService =
                new MailboxQueryService(query, accessor);

        service.delete(delete);

        verify(service).delete(PLAYER_ID, delete);
        assertThat(queryService.list().entries()).isEmpty();
        verify(query).findMailboxEntries(PLAYER_ID);
    }
}
