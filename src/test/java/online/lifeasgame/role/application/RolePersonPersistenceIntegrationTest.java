package online.lifeasgame.role.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.PersonQueryService;
import online.lifeasgame.person.application.PersonService;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.PersonStatus;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.person.infra.JpaPersonRepository;
import online.lifeasgame.role.application.command.RoleCommand;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleStatus;
import online.lifeasgame.role.domain.RoleType;
import online.lifeasgame.role.domain.error.RoleError;
import online.lifeasgame.role.infra.JpaRoleRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
class RolePersonPersistenceIntegrationTest {

    private static final Long OWNER = 23401L;
    private static final Long OTHER_OWNER = 23402L;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_role_person_crud")
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
    private RoleQueryService roleQueryService;

    @Autowired
    private PersonQueryService personQueryService;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    @Autowired
    private JpaRoleRepository roleRepository;

    @Autowired
    private JpaPersonRepository personRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void cleanState() {
        asCurrent(OWNER);
        jdbc.update("DELETE FROM role_relations");
        jdbc.update("DELETE FROM roles");
        jdbc.update("DELETE FROM persons");
    }

    @Test
    void persistsOwnerScopedCrudAndKeepsArchivedRows() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("28");

        var role = roleService.create(
                new RoleCommand.Create("work", "Developer", "Builds")
        );
        asCurrent(OTHER_OWNER);
        var otherRole = roleService.create(
                new RoleCommand.Create("work", "Other", null)
        );
        asCurrent(OWNER);
        assertThat(roleQueryService.list()).extracting(result -> result.id())
                .containsExactly(role.id());
        assertThatThrownBy(() -> roleQueryService.detail(otherRole.id()))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(RoleError.ROLE_NOT_FOUND)
                );
        roleService.update(
                role.id(),
                new RoleCommand.Update("family", "Parent", null)
        );
        roleService.archive(role.id());
        roleService.archive(role.id());
        assertThat(roleQueryService.list()).isEmpty();
        assertThat(rowCount("roles", role.id())).isEqualTo(1);
        assertThatThrownBy(() -> roleService.update(
                role.id(),
                new RoleCommand.Update("SELF", "Self", null)
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(RoleError.ROLE_ARCHIVED)
        );

        var person = personService.create(
                new PersonCommand.Create(
                        "Alice",
                        "Friend",
                        LocalDate.of(2000, 1, 1),
                        "alice@example.com"
                )
        );
        asCurrent(OTHER_OWNER);
        var otherPerson = personService.create(
                new PersonCommand.Create("Other", null, null, null)
        );
        asCurrent(OWNER);
        assertThat(person.linkedUserId()).isNull();
        assertThat(personQueryService.list()).extracting(result -> result.id())
                .containsExactly(person.id());
        assertThatThrownBy(() -> personQueryService.detail(otherPerson.id()))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PersonError.PERSON_NOT_FOUND)
                );
        personService.update(
                person.id(),
                new PersonCommand.Update("Bob", null, null, null)
        );
        personService.archive(person.id());
        personService.archive(person.id());
        assertThat(personQueryService.list()).isEmpty();
        assertThat(rowCount("persons", person.id())).isEqualTo(1);
        assertThatThrownBy(() -> personService.update(
                person.id(),
                new PersonCommand.Update("Carol", null, null, null)
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(PersonError.PERSON_ARCHIVED)
        );
    }

    @Test
    void rejectsCrossOwnerRoleUpdateAndArchive() {
        Long id = roleService.create(
                new RoleCommand.Create("WORK", "Developer", null)
        ).id();
        asCurrent(OTHER_OWNER);

        assertRoleNotFound(() -> roleService.update(
                id,
                new RoleCommand.Update("FAMILY", "Parent", null)
        ));
        assertRoleNotFound(() -> roleService.archive(id));
    }

    @Test
    void rejectsCrossOwnerPersonUpdateAndArchive() {
        Long id = personService.create(
                new PersonCommand.Create("Alice", null, null, null)
        ).id();
        asCurrent(OTHER_OWNER);

        assertPersonNotFound(() -> personService.update(
                id,
                new PersonCommand.Update("Bob", null, null, null)
        ));
        assertPersonNotFound(() -> personService.archive(id));
    }

    @Test
    void rejectsStaleRoleUpdate() {
        Long id = roleService.create(
                new RoleCommand.Create("WORK", "Developer", null)
        ).id();
        Role first = detachedRole(id);
        Role stale = detachedRole(id);

        first.update(RoleType.of("FAMILY"), "Parent", null);
        transaction().executeWithoutResult(status -> roleRepository.saveAndFlush(first));
        stale.update(RoleType.of("SELF"), "Self", null);

        assertThatThrownBy(() -> transaction().executeWithoutResult(
                status -> roleRepository.saveAndFlush(stale)
        )).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void rejectsStaleRoleArchive() {
        Long id = roleService.create(
                new RoleCommand.Create("WORK", "Developer", null)
        ).id();
        Role first = detachedRole(id);
        Role stale = detachedRole(id);

        first.archive();
        transaction().executeWithoutResult(status -> roleRepository.saveAndFlush(first));
        stale.archive();

        assertThatThrownBy(() -> transaction().executeWithoutResult(
                status -> roleRepository.saveAndFlush(stale)
        )).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void rejectsStalePersonUpdate() {
        Long id = personService.create(
                new PersonCommand.Create("Alice", null, null, null)
        ).id();
        Person first = detachedPerson(id);
        Person stale = detachedPerson(id);

        first.update("Bob", null, null, null);
        transaction().executeWithoutResult(status -> personRepository.saveAndFlush(first));
        stale.update("Carol", null, null, null);

        assertThatThrownBy(() -> transaction().executeWithoutResult(
                status -> personRepository.saveAndFlush(stale)
        )).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void rejectsStalePersonArchive() {
        Long id = personService.create(
                new PersonCommand.Create("Alice", null, null, null)
        ).id();
        Person first = detachedPerson(id);
        Person stale = detachedPerson(id);

        first.archive();
        transaction().executeWithoutResult(status -> personRepository.saveAndFlush(first));
        stale.archive();

        assertThatThrownBy(() -> transaction().executeWithoutResult(
                status -> personRepository.saveAndFlush(stale)
        )).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void rollsBackRoleCreateUpdateAndArchive() {
        forceRollback(() -> roleService.create(
                new RoleCommand.Create("WORK", "Rolled back", null)
        ));
        assertThat(roleRepository.count()).isZero();

        Long id = roleService.create(
                new RoleCommand.Create("WORK", "Developer", null)
        ).id();

        forceRollback(() -> roleService.update(
                id,
                new RoleCommand.Update("FAMILY", "Parent", null)
        ));
        assertThat(roleRepository.findById(id).orElseThrow().getName())
                .isEqualTo("Developer");

        forceRollback(() -> roleService.archive(id));
        assertThat(roleRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(RoleStatus.ACTIVE);
    }

    @Test
    void rollsBackPersonCreateUpdateAndArchive() {
        forceRollback(() -> personService.create(
                new PersonCommand.Create("Rolled back", null, null, null)
        ));
        assertThat(personRepository.count()).isZero();

        Long id = personService.create(
                new PersonCommand.Create("Alice", null, null, null)
        ).id();

        forceRollback(() -> personService.update(
                id,
                new PersonCommand.Update("Bob", null, null, null)
        ));
        assertThat(personRepository.findById(id).orElseThrow().getDisplayName())
                .isEqualTo("Alice");

        forceRollback(() -> personService.archive(id));
        assertThat(personRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(PersonStatus.ACTIVE);
    }

    private Role detachedRole(Long id) {
        return transaction().execute(status -> roleRepository.findById(id).orElseThrow());
    }

    private Person detachedPerson(Long id) {
        return transaction().execute(status -> personRepository.findById(id).orElseThrow());
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

    private void assertRoleNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(RoleError.ROLE_NOT_FOUND)
                );
    }

    private void assertPersonNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PersonError.PERSON_NOT_FOUND)
                );
    }

    private int rowCount(String table, Long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Integer.class,
                id
        );
    }

    private void asCurrent(Long playerId) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(playerId);
    }
}
