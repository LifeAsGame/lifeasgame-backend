package online.lifeasgame.role.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.role.application.command.RoleRelationCommand;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleRelation;
import online.lifeasgame.role.domain.RoleRelationType;
import online.lifeasgame.role.domain.RoleType;
import online.lifeasgame.role.domain.repository.RoleRelationRepository;
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

@SpringJUnitConfig(RoleRelationTransactionContractTest.Config.class)
class RoleRelationTransactionContractTest {

    @Autowired
    private RoleRelationReader reader;

    @Autowired
    private RoleRelationWriter writer;

    @Autowired
    private RoleRelationService service;

    @Test
    void readerSupportsWithoutTransactionAndWriterRequiresOne() {
        assertThat(reader.getOwned(4L, 2L, 1L)).isNotNull();
        assertThatThrownBy(() -> writer.saveAndFlush(relation()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void writeServiceOpensTransactionForMandatoryWriter() {
        assertThat(service.create(
                2L,
                new RoleRelationCommand.Create(3L, "FAMILY", null)
        ).status()).isEqualTo("ACTIVE");
    }

    private RoleRelation relation() {
        return Config.relation();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({
            RoleReader.class,
            RoleRelationReader.class,
            RoleRelationWriter.class,
            RoleRelationService.class
    })
    static class Config {

        @Bean
        RoleRepository roleRepository() {
            RoleRepository repository = mock(RoleRepository.class);
            given(repository.findByIdAndPlayerIdForUpdate(any(), any()))
                    .willReturn(Optional.of(role()));
            given(repository.findByIdAndPlayerId(any(), any()))
                    .willReturn(Optional.of(role()));
            return repository;
        }

        @Bean
        RoleRelationRepository relationRepository() {
            RoleRelationRepository repository = mock(RoleRelationRepository.class);
            given(repository.findByIdAndRoleIdAndPlayerId(any(), any(), any()))
                    .willReturn(Optional.of(relation()));
            given(repository.findByRoleIdAndPersonIdAndPlayerId(any(), any(), any()))
                    .willReturn(Optional.empty());
            given(repository.saveAndFlush(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));
            return repository;
        }

        @Bean
        PersonLookupApi personLookupApi() {
            PersonLookupApi api = mock(PersonLookupApi.class);
            given(api.getOwnedActive(any(), any())).willReturn(
                    new PersonLookupApi.PersonReference(3L, null, "Alice")
            );
            return api;
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

        private static Role role() {
            return Role.create(1L, RoleType.of("SELF"), "Self", null);
        }

        private static RoleRelation relation() {
            return RoleRelation.create(
                    1L,
                    2L,
                    3L,
                    RoleRelationType.of("FAMILY"),
                    null
            );
        }
    }
}
