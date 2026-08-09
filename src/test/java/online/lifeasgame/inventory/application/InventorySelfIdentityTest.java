package online.lifeasgame.inventory.application;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.query.InventoryQuery;
import online.lifeasgame.inventory.application.query.MailboxQuery;
import online.lifeasgame.inventory.application.result.InventoryResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("self Inventory와 Mailbox use case")
class InventorySelfIdentityTest {

    private static final Long PLAYER_ID = 247L;

    @Nested
    @DisplayName("Inventory를 사용할 때")
    class InventoryUseCase {

        private CurrentPlayerAccessor accessor;
        private InventoryService service;
        private InventoryQuery query;
        private InventoryQueryService queryService;

        @BeforeEach
        void setUp() {
            accessor = mock(CurrentPlayerAccessor.class);
            when(accessor.currentPlayerIdOrThrow()).thenReturn(PLAYER_ID);
            service = spy(new InventoryService(
                    mock(InventoryReader.class),
                    mock(ItemReader.class),
                    mock(DomainEventPublisher.class),
                    accessor
            ));
            query = mock(InventoryQuery.class);
            queryService = new InventoryQueryService(query, accessor);
        }

        @Test
        @DisplayName("command는 현재 Player identity로 실행한다")
        void resolvesCurrentPlayerForCommand() {
            InventoryCommand.Remove remove =
                    new InventoryCommand.Remove(1, 1);
            doNothing().when(service).remove(PLAYER_ID, remove);

            service.remove(remove);

            verify(service).remove(PLAYER_ID, remove);
        }

        @Test
        @DisplayName("query는 현재 Player identity로 projection을 조회한다")
        void resolvesCurrentPlayerForQuery() {
            when(query.findInventoryEntries(PLAYER_ID))
                    .thenReturn(List.of());

            InventoryResult.Entries entries = queryService.list();

            verify(query).findInventoryEntries(PLAYER_ID);
            assertThat(entries.entryViews()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Mailbox를 사용할 때")
    class MailboxUseCase {

        private CurrentPlayerAccessor accessor;
        private MailboxService service;
        private MailboxQuery query;
        private MailboxQueryService queryService;

        @BeforeEach
        void setUp() {
            accessor = mock(CurrentPlayerAccessor.class);
            when(accessor.currentPlayerIdOrThrow()).thenReturn(PLAYER_ID);
            service = spy(new MailboxService(
                    mock(MailboxReader.class),
                    mock(InventoryReader.class),
                    mock(ItemReader.class),
                    accessor
            ));
            query = mock(MailboxQuery.class);
            queryService = new MailboxQueryService(query, accessor);
        }

        @Test
        @DisplayName("command는 현재 Player identity로 실행한다")
        void resolvesCurrentPlayerForCommand() {
            MailboxCommand.Delete delete = new MailboxCommand.Delete(1);
            doNothing().when(service).delete(PLAYER_ID, delete);

            service.delete(delete);

            verify(service).delete(PLAYER_ID, delete);
        }

        @Test
        @DisplayName("query는 현재 Player identity로 projection을 조회한다")
        void resolvesCurrentPlayerForQuery() {
            when(query.findMailboxEntries(PLAYER_ID))
                    .thenReturn(List.of());

            assertThat(queryService.list().entries()).isEmpty();

            verify(query).findMailboxEntries(PLAYER_ID);
        }
    }
}
