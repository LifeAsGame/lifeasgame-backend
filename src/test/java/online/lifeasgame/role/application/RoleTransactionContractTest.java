package online.lifeasgame.role.application;

import online.lifeasgame.role.application.command.RoleCommand;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.repository.RoleRepository;
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

@SpringJUnitConfig(RoleTransactionContractTest.Config.class)
class RoleTransactionContractTest {

    @Autowired
    private RoleReader reader;

    @Autowired
    private RoleWriter writer;

    @Autowired
    private RoleService service;

    @Test
    void readerSupportsWithoutTransactionAndWriterRequiresOne() {
        assertThat(reader.getOwned(1L, 1L)).isNotNull();
        assertThatThrownBy(() -> writer.save(role()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void writeServiceOpensTransactionForMandatoryWriter() {
        assertThat(service.create(
                new RoleCommand.Create("WORK", "Developer", null)
        ).status()).isEqualTo("ACTIVE");
    }

    private Role role() {
        return Role.create(
                1L,
                online.lifeasgame.role.domain.RoleType.of("WORK"),
                "Developer",
                null
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({RoleReader.class, RoleWriter.class, RoleService.class})
    static class Config {

        @Bean
        RoleRepository repository() {
            RoleRepository repository = mock(RoleRepository.class);
            given(repository.findByIdAndPlayerId(any(), any()))
                    .willReturn(Optional.of(role()));
            given(repository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
            return repository;
        }

        private Role role() {
            return Role.create(
                    1L,
                    online.lifeasgame.role.domain.RoleType.of("WORK"),
                    "Developer",
                    null
            );
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
