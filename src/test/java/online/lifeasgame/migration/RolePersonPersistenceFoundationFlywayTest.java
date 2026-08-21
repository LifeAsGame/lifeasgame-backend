package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("V19 Role/Person persistence foundation migration")
class RolePersonPersistenceFoundationFlywayTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_role_person")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    private Flyway flyway;
    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateCleanDatabase() {
        flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()
        ));
    }

    @Test
    @DisplayName("V27까지 적용된 schema에서 V19 Role/Person 계약을 고정한다")
    void createsSchemaContract() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("27");
        assertThat(tableContracts()).containsExactly(
                new TableContract("roles", "InnoDB", "utf8mb4_0900_ai_ci"),
                new TableContract("persons", "InnoDB", "utf8mb4_0900_ai_ci"),
                new TableContract("role_relations", "InnoDB", "utf8mb4_0900_ai_ci")
        );
        assertThat(columnContracts()).containsExactly(
                column("roles", "id", "bigint", "NO", null, "auto_increment"),
                column("roles", "player_id", "bigint", "NO", null, ""),
                column("roles", "role_type", "varchar(40)", "NO", null, ""),
                column("roles", "name", "varchar(60)", "NO", null, ""),
                column("roles", "description", "varchar(500)", "YES", null, ""),
                column("roles", "status", "varchar(20)", "NO", "ACTIVE", ""),
                column("roles", "created_at", "datetime(6)", "NO", null, ""),
                column("roles", "updated_at", "datetime(6)", "NO", null, ""),
                column("roles", "version", "bigint", "NO", "0", ""),
                column("persons", "id", "bigint", "NO", null, "auto_increment"),
                column("persons", "owner_player_id", "bigint", "NO", null, ""),
                column("persons", "linked_user_id", "bigint", "YES", null, ""),
                column("persons", "display_name", "varchar(80)", "NO", null, ""),
                column("persons", "notes", "text", "YES", null, ""),
                column("persons", "birthday", "date", "YES", null, ""),
                column("persons", "contact", "varchar(120)", "YES", null, ""),
                column("persons", "status", "varchar(20)", "NO", "ACTIVE", ""),
                column("persons", "created_at", "datetime(6)", "NO", null, ""),
                column("persons", "updated_at", "datetime(6)", "NO", null, ""),
                column("persons", "version", "bigint", "NO", "0", ""),
                column("role_relations", "id", "bigint", "NO", null, "auto_increment"),
                column("role_relations", "player_id", "bigint", "NO", null, ""),
                column("role_relations", "role_id", "bigint", "NO", null, ""),
                column("role_relations", "person_id", "bigint", "NO", null, ""),
                column("role_relations", "relation_type", "varchar(40)", "NO", null, ""),
                column("role_relations", "role_notes", "text", "YES", null, ""),
                column("role_relations", "status", "varchar(20)", "NO", "ACTIVE", ""),
                column("role_relations", "created_at", "datetime(6)", "NO", null, ""),
                column("role_relations", "updated_at", "datetime(6)", "NO", null, ""),
                column("role_relations", "version", "bigint", "NO", "0", "")
        );

        assertIndex("roles", "PRIMARY", false, "id");
        assertIndex("roles", "uq_role_id_player", false, "id", "player_id");
        assertIndex("roles", "idx_role_player_status", true, "player_id", "status", "id");
        assertIndex("persons", "PRIMARY", false, "id");
        assertIndex("persons", "uq_person_id_owner", false, "id", "owner_player_id");
        assertIndex("persons", "uq_person_owner_linked_user", false,
                "owner_player_id", "linked_user_id");
        assertIndex("persons", "idx_person_owner_status", true,
                "owner_player_id", "status", "id");
        assertIndex("role_relations", "PRIMARY", false, "id");
        assertIndex("role_relations", "uq_role_relation_role_person", false,
                "role_id", "person_id");
        assertIndex("role_relations", "idx_role_relation_player_role_status", true,
                "player_id", "role_id", "status", "id");
        assertIndex("role_relations", "idx_role_relation_player_person_status", true,
                "player_id", "person_id", "status", "id");

        assertThat(checkConstraints()).containsExactlyInAnyOrder(
                "ck_role_player", "ck_role_type", "ck_role_name", "ck_role_status",
                "ck_person_owner", "ck_person_linked_user", "ck_person_display_name",
                "ck_person_status", "ck_role_relation_player", "ck_role_relation_role",
                "ck_role_relation_person", "ck_role_relation_type", "ck_role_relation_status"
        );
        assertThat(foreignKeys()).containsExactlyInAnyOrder(
                new ForeignKeyContract(
                        "fk_role_relation_role_owner",
                        "role_id,player_id",
                        "roles",
                        "id,player_id",
                        "RESTRICT",
                        "RESTRICT"
                ),
                new ForeignKeyContract(
                        "fk_role_relation_person_owner",
                        "person_id,player_id",
                        "persons",
                        "id,owner_player_id",
                        "RESTRICT",
                        "RESTRICT"
                )
        );
    }

    @Test
    @DisplayName("Player ownership과 Person-linked User 분리 invariant를 DB constraint로 보장한다")
    void enforcesOwnershipAndOptionalLinkedUser() {
        insertRole(1001, 101, "Self");
        insertRole(1002, 202, "Other");
        insertPerson(2001, 101, 9001L, "Linked self");
        insertPerson(2002, 202, 9001L, "Linked other");
        insertPerson(2003, 101, null, "Unlinked one");
        insertPerson(2004, 101, null, "Unlinked two");

        assertThatThrownBy(() -> insertPerson(2005, 101, 9001L, "Duplicate link"))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertRelation(3001, 101, 1001, 2001);
        assertThatThrownBy(() -> insertRelation(3002, 101, 1001, 2001))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRelation(3003, 101, 1002, 2001))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRelation(3004, 101, 1001, 2002))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM roles WHERE id = 1001"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM persons WHERE id = 2001"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM persons WHERE linked_user_id IS NULL",
                Integer.class
        )).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM persons WHERE linked_user_id = 9001",
                Integer.class
        )).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_relations",
                Integer.class
        )).isEqualTo(1);
    }

    private List<TableContract> tableContracts() {
        return jdbc.query("""
                SELECT table_name, engine, table_collation
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('roles', 'persons', 'role_relations')
                ORDER BY FIELD(table_name, 'roles', 'persons', 'role_relations')
                """, (resultSet, rowNumber) -> new TableContract(
                resultSet.getString("table_name"),
                resultSet.getString("engine"),
                resultSet.getString("table_collation")
        ));
    }

    private List<ColumnContract> columnContracts() {
        return jdbc.query("""
                SELECT table_name, column_name, column_type, is_nullable,
                       column_default, extra
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name IN ('roles', 'persons', 'role_relations')
                ORDER BY FIELD(table_name, 'roles', 'persons', 'role_relations'),
                         ordinal_position
                """, (resultSet, rowNumber) -> new ColumnContract(
                resultSet.getString("table_name"),
                resultSet.getString("column_name"),
                resultSet.getString("column_type"),
                resultSet.getString("is_nullable"),
                resultSet.getString("column_default"),
                resultSet.getString("extra")
        ));
    }

    private ColumnContract column(
            String tableName,
            String columnName,
            String columnType,
            String nullable,
            String defaultValue,
            String extra
    ) {
        return new ColumnContract(
                tableName, columnName, columnType, nullable, defaultValue, extra
        );
    }

    private void assertIndex(
            String tableName,
            String indexName,
            boolean nonUnique,
            String... columns
    ) {
        List<IndexColumn> actual = jdbc.query("""
                SELECT column_name, non_unique
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                ORDER BY seq_in_index
                """, (resultSet, rowNumber) -> new IndexColumn(
                resultSet.getString("column_name"),
                resultSet.getBoolean("non_unique")
        ), tableName, indexName);
        assertThat(actual).containsExactly(
                java.util.Arrays.stream(columns)
                        .map(column -> new IndexColumn(column, nonUnique))
                        .toArray(IndexColumn[]::new)
        );
    }

    private Set<String> checkConstraints() {
        return Set.copyOf(jdbc.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name IN ('roles', 'persons', 'role_relations')
                  AND constraint_type = 'CHECK'
                """, String.class));
    }

    private List<ForeignKeyContract> foreignKeys() {
        return jdbc.query("""
                SELECT
                    kcu.constraint_name,
                    GROUP_CONCAT(kcu.column_name ORDER BY kcu.ordinal_position) AS fk_columns,
                    kcu.referenced_table_name,
                    GROUP_CONCAT(
                        kcu.referenced_column_name ORDER BY kcu.ordinal_position
                    ) AS referenced_columns,
                    rc.update_rule,
                    rc.delete_rule
                FROM information_schema.key_column_usage kcu
                JOIN information_schema.referential_constraints rc
                  ON rc.constraint_schema = kcu.constraint_schema
                 AND rc.constraint_name = kcu.constraint_name
                 AND rc.table_name = kcu.table_name
                WHERE kcu.table_schema = DATABASE()
                  AND kcu.table_name IN ('roles', 'persons', 'role_relations')
                  AND kcu.referenced_table_name IS NOT NULL
                GROUP BY kcu.constraint_name, kcu.referenced_table_name,
                         rc.update_rule, rc.delete_rule
                """, (resultSet, rowNumber) -> new ForeignKeyContract(
                resultSet.getString("constraint_name"),
                resultSet.getString("fk_columns"),
                resultSet.getString("referenced_table_name"),
                resultSet.getString("referenced_columns"),
                resultSet.getString("update_rule"),
                resultSet.getString("delete_rule")
        ));
    }

    private void insertRole(long id, long playerId, String name) {
        jdbc.update("""
                INSERT INTO roles (
                    id, player_id, role_type, name, created_at, updated_at
                ) VALUES (?, ?, 'PERSONAL', ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, playerId, name);
    }

    private void insertPerson(
            long id,
            long ownerPlayerId,
            Long linkedUserId,
            String displayName
    ) {
        jdbc.update("""
                INSERT INTO persons (
                    id, owner_player_id, linked_user_id, display_name,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, ownerPlayerId, linkedUserId, displayName);
    }

    private void insertRelation(
            long id,
            long playerId,
            long roleId,
            long personId
    ) {
        jdbc.update("""
                INSERT INTO role_relations (
                    id, player_id, role_id, person_id, relation_type,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'KNOWN_AS', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, id, playerId, roleId, personId);
    }

    private record TableContract(
            String tableName,
            String engine,
            String collation
    ) {
    }

    private record ColumnContract(
            String tableName,
            String columnName,
            String columnType,
            String nullable,
            String defaultValue,
            String extra
    ) {
    }

    private record IndexColumn(String name, boolean nonUnique) {
    }

    private record ForeignKeyContract(
            String name,
            String columns,
            String referencedTable,
            String referencedColumns,
            String updateRule,
            String deleteRule
    ) {
    }
}
