package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("V39 데이터에서 RoleParty V40으로 확장할 때")
class RolePartyUpgradeFlywayTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_role_party_upgrade")
            .withUsername("lifeasgame").withPassword("lifeasgame");

    @Test
    @DisplayName("기존 Player·Role와 V38/V39 checksum을 보존한다")
    void preservesExistingDataAndChecksums() {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").target(MigrationVersion.fromVersion("39")).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        jdbc.update("""
                INSERT INTO player (id, user_id, name, level, exp, hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat, extra_stats,
                    status_effects, version, created_at, updated_at)
                VALUES (61201, 61201, 'fixture', 1, 0, 100, 100, 50, 50, 1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0, NOW(6), NOW(6))
                """);
        jdbc.update("""
                INSERT INTO roles (player_id, role_type, name, status, created_at, updated_at, version)
                VALUES (61201, 'WORK', 'preserved', 'ACTIVE', NOW(6), NOW(6), 0)
                """);
        var before = jdbc.queryForList("SELECT version, checksum FROM flyway_schema_history WHERE version IN ('38','39') ORDER BY installed_rank");
        Flyway latest = Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").load();
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);
        latest.validate();
        assertThat(jdbc.queryForList("SELECT version, checksum FROM flyway_schema_history WHERE version IN ('38','39') ORDER BY installed_rank"))
                .isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT name FROM roles WHERE player_id = 61201", String.class)).isEqualTo("preserved");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('role_parties','role_party_members','role_party_invitations')", Long.class))
                .isEqualTo(3);
    }
}
