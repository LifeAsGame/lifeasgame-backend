package online.lifeasgame.inventory.application;

import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import online.lifeasgame.inventory.domain.repository.PlayerInventoryRepository;
import online.lifeasgame.inventory.domain.repository.PlayerMailboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(ReaderLockingTransactionContractTest.Config.class)
class ReaderLockingTransactionContractTest {

    private static final Long PLAYER_ID = 229L;

    @Autowired
    private InventoryReader inventoryReader;

    @Autowired
    private MailboxReader mailboxReader;

    @Autowired
    private PlayerInventoryRepository inventoryRepository;

    @Autowired
    private PlayerMailboxRepository mailboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void rejectsLockingReadersWithoutTransaction() {
        assertThatThrownBy(() ->
                inventoryReader.getByPlayerIdForUpdateOrThrow(PLAYER_ID)
        ).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() ->
                mailboxReader.getByPlayerIdForUpdateOrThrow(PLAYER_ID)
        ).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void allowsLockingReadersInsideTransaction() {
        PlayerInventory inventory = mock(PlayerInventory.class);
        PlayerMailbox mailbox = mock(PlayerMailbox.class);
        given(inventoryRepository.findByPlayerIdForUpdate(PLAYER_ID))
                .willReturn(Optional.of(inventory));
        given(mailboxRepository.findByPlayerIdForUpdate(PLAYER_ID))
                .willReturn(Optional.of(mailbox));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(inventoryReader.getByPlayerIdForUpdateOrThrow(PLAYER_ID))
                    .isSameAs(inventory);
            assertThat(mailboxReader.getByPlayerIdForUpdateOrThrow(PLAYER_ID))
                    .isSameAs(mailbox);
        });
    }

    @Test
    void keepsRegularReadersUsableWithoutTransaction() {
        PlayerInventory inventory = mock(PlayerInventory.class);
        PlayerMailbox mailbox = mock(PlayerMailbox.class);
        given(inventoryRepository.findByPlayerId(PLAYER_ID))
                .willReturn(Optional.of(inventory));
        given(mailboxRepository.findByPlayerId(PLAYER_ID))
                .willReturn(Optional.of(mailbox));

        assertThat(inventoryReader.getByPlayerIdOrThrow(PLAYER_ID))
                .isSameAs(inventory);
        assertThat(mailboxReader.getByPlayerIdOrThrow(PLAYER_ID))
                .isSameAs(mailbox);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({InventoryReader.class, MailboxReader.class})
    static class Config {

        @Bean
        PlayerInventoryRepository inventoryRepository() {
            return mock(PlayerInventoryRepository.class);
        }

        @Bean
        PlayerMailboxRepository mailboxRepository() {
            return mock(PlayerMailboxRepository.class);
        }

        @Bean
        EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        PlatformTransactionManager transactionManager(
                EmbeddedDatabase dataSource
        ) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
