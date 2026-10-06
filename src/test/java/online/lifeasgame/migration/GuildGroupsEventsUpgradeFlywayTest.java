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
@DisplayName("기존 V40 데이터에 Guild 연결·행사 V41을 적용할 때")
class GuildGroupsEventsUpgradeFlywayTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_guild_v41_upgrade")
            .withUsername("lifeasgame").withPassword("lifeasgame");

    @Test
    @DisplayName("Player·Guild·Party·RoleParty와 기존 checksum을 보존한다")
    void preservesExistingData() {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").target(MigrationVersion.fromVersion("40")).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        jdbc.update("""
                INSERT INTO player (id, user_id, name, level, exp, hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat, extra_stats,
                    status_effects, version, created_at, updated_at)
                VALUES (62201, 62201, 'preserved', 1, 0, 100, 100, 50, 50, 1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0, NOW(6), NOW(6))
                """);
        jdbc.update("""
                INSERT INTO guilds (guild_id, player_id, leader_player_id, name_original, name_value,
                    code_value, visibility, join_policy, status, max_members, created_at, updated_at)
                VALUES (62201, 62201, 62201, 'preserved', 'preserved', 'V41-GUILD', 'PRIVATE', 'APPROVAL', 'ACTIVE', 10, NOW(6), NOW(6))
                """);
        jdbc.update("""
                INSERT INTO parties (party_id, player_id, leader_player_id, name_original, name_value,
                    code_value, visibility, join_policy, status, max_members, created_at, updated_at)
                VALUES (62201, 62201, 62201, 'preserved', 'preserved', 'V41-PARTY', 'PRIVATE', 'APPROVAL', 'ACTIVE', 10, NOW(6), NOW(6))
                """);
        jdbc.update("""
                INSERT INTO roles (player_id, role_type, name, status, created_at, updated_at, version)
                VALUES (62201, 'WORK', 'preserved', 'ACTIVE', NOW(6), NOW(6), 0)
                """);
        long roleId = jdbc.queryForObject("SELECT id FROM roles WHERE player_id = 62201", Long.class);
        jdbc.update("""
                INSERT INTO role_parties (role_id, creator_player_id, leader_player_id, name, status,
                    max_members, version, created_at, updated_at)
                VALUES (?, 62201, 62201, 'preserved', 'ACTIVE', 3, 0, NOW(6), NOW(6))
                """, roleId);
        var before = jdbc.queryForList("SELECT version, checksum FROM flyway_schema_history WHERE version IN ('38','39','40') ORDER BY installed_rank");
        Flyway latest = Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").load();
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(9);
        latest.validate();
        assertThat(jdbc.queryForList("SELECT version, checksum FROM flyway_schema_history WHERE version IN ('38','39','40') ORDER BY installed_rank"))
                .isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guilds WHERE guild_id = 62201", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parties WHERE party_id = 62201", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_parties WHERE creator_player_id = 62201", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('guild_group_links','guild_events','guild_event_rsvps')", Long.class))
                .isEqualTo(3);
    }
}
