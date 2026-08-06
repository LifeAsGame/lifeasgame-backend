package online.lifeasgame.person.application;

import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.repository.PersonRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(PersonTransactionContractTest.Config.class)
class PersonTransactionContractTest {

    @Autowired
    private PersonReader reader;

    @Autowired
    private PersonWriter writer;

    @Autowired
    private PersonService service;

    @Test
    void readerSupportsWithoutTransactionAndWriterRequiresOne() {
        assertThat(reader.getOwned(1L, 1L)).isNotNull();
        assertThatThrownBy(() -> writer.save(person()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void writeServiceOpensTransactionForMandatoryWriter() {
        assertThat(service.create(
                new PersonCommand.Create("Alice", null, null, null)
        ).status()).isEqualTo("ACTIVE");
    }

    private Person person() {
        return Person.create(1L, "Alice", null, null, null);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({PersonReader.class, PersonWriter.class, PersonService.class})
    static class Config {

        @Bean
        PersonRepository repository() {
            PersonRepository repository = mock(PersonRepository.class);
            given(repository.findByIdAndOwnerPlayerId(any(), any()))
                    .willReturn(Optional.of(person()));
            given(repository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
            return repository;
        }

        private Person person() {
            return Person.create(1L, "Alice", null, null, null);
        }

        @Bean
        CurrentPlayerAccessor currentPlayerAccessor() {
            return () -> Optional.of(1L);
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
