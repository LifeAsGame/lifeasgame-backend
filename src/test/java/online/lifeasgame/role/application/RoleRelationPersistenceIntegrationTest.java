package online.lifeasgame.role.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.PersonService;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.role.application.command.RoleCommand;
import online.lifeasgame.role.application.command.RoleRelationCommand;
import online.lifeasgame.role.domain.RoleRelation;
import online.lifeasgame.role.domain.RoleRelationStatus;
import online.lifeasgame.role.domain.RoleRelationType;
import online.lifeasgame.role.domain.error.RoleError;
import online.lifeasgame.role.infra.JpaRoleRelationRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
class RoleRelationPersistenceIntegrationTest {

    private static final Long OWNER = 23701L;
    private static final Long OTHER_OWNER = 23702L;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_role_relation_crud")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private RoleService roleService;

    @Autowired
    private PersonService personService;

    @Autowired
    private RoleRelationService relationService;

    @Autowired
    private RoleRelationQueryService relationQueryService;

    @Autowired
    private JpaRoleRelationRepository relationRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Flyway flyway;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    @BeforeEach
    void cleanState() {
        asCurrent(OWNER);
        jdbc.update("DELETE FROM role_relations");
        jdbc.update("DELETE FROM roles");
        jdbc.update("DELETE FROM persons");
    }

    @Test
    void persistsCrudRejectsActiveDuplicateAndReactivatesArchivedRow() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("28");
        Long roleId = createRole("Owner role");
        Long personId = createPerson("Alice");

        var created = relationService.create(
                roleId,
                new RoleRelationCommand.Create(personId, " family ", " note ")
        );
        assertThat(created.relationType()).isEqualTo("FAMILY");
        assertThat(created.personDisplayName()).isEqualTo("Alice");
        assertThat(relationQueryService.list(roleId)).extracting(result -> result.id())
                .containsExactly(created.id());

        assertDomainError(
                () -> relationService.create(
                        roleId,
                        new RoleRelationCommand.Create(personId, "FRIEND", null)
                ),
                RoleError.ROLE_RELATION_ALREADY_EXISTS
        );

        relationService.archive(roleId, created.id());
        relationService.archive(roleId, created.id());
        assertThat(relationQueryService.list(roleId)).isEmpty();
        assertThat(relationRepository.count()).isEqualTo(1);

        var reactivated = relationService.create(
                roleId,
                new RoleRelationCommand.Create(personId, "FRIEND", "again")
        );
        assertThat(reactivated.id()).isEqualTo(created.id());
        assertThat(reactivated.status()).isEqualTo("ACTIVE");
        assertThat(reactivated.relationType()).isEqualTo("FRIEND");
        assertThat(relationRepository.count()).isEqualTo(1);
    }

    @Test
    void enforcesCurrentPlayerOwnershipAndArchivedProviderState() {
        Long roleId = createRole("Owner role");
        Long personId = createPerson("Owner person");
        Long relationId = relationService.create(
                roleId,
                new RoleRelationCommand.Create(personId, "FRIEND", null)
        ).id();
        Long secondRoleId = createRole("Second role");

        asCurrent(OTHER_OWNER);
        Long otherRoleId = createRole("Other role");
        Long otherPersonId = createPerson("Other person");
        asCurrent(OWNER);

        assertDomainError(
                () -> relationService.create(
                        otherRoleId,
                        new RoleRelationCommand.Create(personId, "FRIEND", null)
                ),
                RoleError.ROLE_NOT_FOUND
        );
        assertDomainError(
                () -> relationService.create(
                        roleId,
                        new RoleRelationCommand.Create(otherPersonId, "FRIEND", null)
                ),
                PersonError.PERSON_NOT_FOUND
        );
        assertDomainError(
                () -> relationService.update(
                        secondRoleId,
                        relationId,
                        new RoleRelationCommand.Update("FAMILY", null)
                ),
                RoleError.ROLE_RELATION_NOT_FOUND
        );

        personService.archive(personId);
        relationService.archive(roleId, relationId);
        assertDomainError(
                () -> relationService.create(
                        roleId,
                        new RoleRelationCommand.Create(personId, "FAMILY", null)
                ),
                PersonError.PERSON_ARCHIVED
        );
    }

    @Test
    void v19ConstraintsRejectDuplicatePairAndCrossOwnerPerson() {
        Long roleId = createRole("Owner role");
        Long personId = createPerson("Owner person");
        relationService.create(
                roleId,
                new RoleRelationCommand.Create(personId, "FRIEND", null)
        );

        RoleRelation duplicate = RoleRelation.create(
                OWNER,
                roleId,
                personId,
                RoleRelationType.of("FAMILY"),
                null
        );
        assertThatThrownBy(() -> transaction().executeWithoutResult(
                status -> relationRepository.saveAndFlush(duplicate)
        )).isInstanceOf(DataIntegrityViolationException.class);

        asCurrent(OTHER_OWNER);
        Long otherPersonId = createPerson("Other person");
        RoleRelation crossOwner = RoleRelation.create(
                OWNER,
                roleId,
                otherPersonId,
                RoleRelationType.of("FRIEND"),
                null
        );
        assertThatThrownBy(() -> transaction().executeWithoutResult(
                status -> relationRepository.saveAndFlush(crossOwner)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesOptimisticLockingAndRollsBackWrites() {
        Long roleId = createRole("Owner role");
        Long personId = createPerson("Alice");
        Long relationId = relationService.create(
                roleId,
                new RoleRelationCommand.Create(personId, "FRIEND", null)
        ).id();
        RoleRelation updateFirst = detachedRelation(relationId);
        RoleRelation updateStale = detachedRelation(relationId);

        updateFirst.update(RoleRelationType.of("FAMILY"), null);
        transaction().executeWithoutResult(
                status -> relationRepository.saveAndFlush(updateFirst)
        );
        updateStale.update(RoleRelationType.of("MENTOR"), null);
        assertThatThrownBy(() -> transaction().executeWithoutResult(
                status -> relationRepository.saveAndFlush(updateStale)
        )).isInstanceOf(OptimisticLockingFailureException.class);

        Long archivePersonId = createPerson("Archive target");
        Long archiveRelationId = relationService.create(
                roleId,
                new RoleRelationCommand.Create(archivePersonId, "FRIEND", null)
        ).id();
        RoleRelation archiveFirst = detachedRelation(archiveRelationId);
        RoleRelation archiveStale = detachedRelation(archiveRelationId);
        archiveFirst.archive();
        transaction().executeWithoutResult(
                status -> relationRepository.saveAndFlush(archiveFirst)
        );
        archiveStale.archive();
        assertThatThrownBy(() -> transaction().executeWithoutResult(
                status -> relationRepository.saveAndFlush(archiveStale)
        )).isInstanceOf(OptimisticLockingFailureException.class);

        Long rollbackPersonId = createPerson("Bob");
        forceRollback(() -> relationService.create(
                roleId,
                new RoleRelationCommand.Create(rollbackPersonId, "FRIEND", null)
        ));
        assertThat(relationRepository.findByRoleIdAndPersonIdAndPlayerId(
                roleId,
                rollbackPersonId,
                OWNER
        )).isEmpty();

        Long mutableId = relationService.create(
                roleId,
                new RoleRelationCommand.Create(rollbackPersonId, "FRIEND", null)
        ).id();
        forceRollback(() -> relationService.update(
                roleId,
                mutableId,
                new RoleRelationCommand.Update("FAMILY", null)
        ));
        assertThat(relationRepository.findById(mutableId).orElseThrow()
                .getRelationType().value()).isEqualTo("FRIEND");

        forceRollback(() -> relationService.archive(roleId, mutableId));
        assertThat(relationRepository.findById(mutableId).orElseThrow().getStatus())
                .isEqualTo(RoleRelationStatus.ACTIVE);
    }

    private Long createRole(String name) {
        return roleService.create(new RoleCommand.Create("WORK", name, null)).id();
    }

    private Long createPerson(String name) {
        return personService.create(new PersonCommand.Create(name, null, null, null)).id();
    }

    private RoleRelation detachedRelation(Long id) {
        return transaction().execute(status -> relationRepository.findById(id).orElseThrow());
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private void forceRollback(Runnable action) {
        assertThatThrownBy(() -> transaction().executeWithoutResult(status -> {
            action.run();
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class);
    }

    private void assertDomainError(Runnable action, Object expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expected)
                );
    }

    private void asCurrent(Long playerId) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(playerId);
    }
}
