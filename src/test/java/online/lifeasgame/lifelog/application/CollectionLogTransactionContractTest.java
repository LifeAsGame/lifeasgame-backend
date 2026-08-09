package online.lifeasgame.lifelog.application;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
import online.lifeasgame.lifelog.domain.repository.CollectionLogRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(CollectionLogTransactionContractTest.Config.class)
class CollectionLogTransactionContractTest {

    private static final Long PLAYER_ID = 241L;
    private static final Long COLLECTION_ID = 24L;

    @Autowired
    private CollectionLogService collectionLogService;

    @Autowired
    private CollectionLogWriter collectionLogWriter;

    @Autowired
    private CollectionLogRepository repository;

    @Test
    void selfDeleteOpensTransactionForMandatoryWriter() {
        collectionLogService.delete(COLLECTION_ID);

        verify(repository).deleteByIdAndPlayerId(COLLECTION_ID, PLAYER_ID);
    }

    @Test
    void adminDeleteKeepsExplicitPlayerAndTransaction() {
        collectionLogService.delete(242L, COLLECTION_ID);

        verify(repository).deleteByIdAndPlayerId(COLLECTION_ID, 242L);
    }

    @Test
    void writerStillRejectsDeleteWithoutTransaction() {
        assertThatThrownBy(() -> collectionLogWriter.delete(
                PLAYER_ID,
                COLLECTION_ID
        )).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({CollectionLogService.class, CollectionLogWriter.class})
    static class Config {

        @Bean
        CollectionLogReader collectionLogReader() {
            return mock(CollectionLogReader.class);
        }

        @Bean
        CollectionLogRepository collectionLogRepository() {
            return mock(CollectionLogRepository.class);
        }

        @Bean
        LifeLogRecordRegistrar lifeLogRecordRegistrar() {
            return mock(LifeLogRecordRegistrar.class);
        }

        @Bean
        DomainEventPublisher domainEventPublisher() {
            return mock(DomainEventPublisher.class);
        }

        @Bean
        CurrentPlayerAccessor currentPlayerAccessor() {
            return () -> Optional.of(PLAYER_ID);
        }

        @Bean
        EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        PlatformTransactionManager transactionManager(EmbeddedDatabase dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
