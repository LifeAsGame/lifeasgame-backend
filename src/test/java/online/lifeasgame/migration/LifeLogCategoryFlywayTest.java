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
@DisplayName("LifeLog 분류 V45")
class LifeLogCategoryFlywayTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39");

    @Test
    @DisplayName("기존 사용 코드만 보존하고 숨김·삭제가 원본 기록을 지우지 않는다")
    void preservesUsedSystemCodesAndSources() {
        flyway("44").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        jdbc.update("""
                INSERT INTO player (id, user_id, name, level, exp, hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat, extra_stats,
                    status_effects, version, created_at, updated_at)
                VALUES (78101, 78101, 'existing', 1, 0, 100, 100, 50, 50, 1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0, NOW(6), NOW(6))
                """);
        jdbc.update("""
                INSERT INTO collection_logs (id, player_id, category, title_value, quantity_value, created_at, updated_at)
                VALUES (78101, 78101, 'PROJECT', 'evidence', 1, NOW(6), NOW(6))
                """);
        flyway(null).migrate();
        assertThat(jdbc.queryForList(
                "SELECT system_code FROM lifelog_categories WHERE owner_player_id=78101", String.class))
                .containsExactly("PROJECT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lifelog_categories WHERE owner_player_id=78102", Integer.class))
                .isZero();

        Long systemId = jdbc.queryForObject("SELECT id FROM lifelog_categories WHERE owner_player_id=78101", Long.class);
        jdbc.update("UPDATE lifelog_categories SET hidden=b'1' WHERE id=?", systemId);
        assertThat(jdbc.queryForObject("SELECT hidden FROM lifelog_categories WHERE id=?", Boolean.class, systemId))
                .isTrue();
        jdbc.update("""
                INSERT INTO lifelog_categories (owner_player_id,kind,source,system_code,hidden)
                VALUES (78101,'COLLECTION','SYSTEM','PROJECT',b'0')
                ON DUPLICATE KEY UPDATE hidden=b'0'
                """);
        assertThat(jdbc.queryForObject("SELECT id FROM lifelog_categories WHERE owner_player_id=78101", Long.class))
                .isEqualTo(systemId);

        jdbc.update("""
                INSERT INTO lifelog_categories (owner_player_id,kind,source,name,normalized_name,hidden)
                VALUES (78101,'COLLECTION','PERSONAL','My Project','my project',b'0')
                """);
        Long personalId = jdbc.queryForObject(
                "SELECT id FROM lifelog_categories WHERE normalized_name='my project'", Long.class);
        jdbc.update("UPDATE collection_logs SET personal_category_id=? WHERE id=78101", personalId);
        jdbc.update("DELETE FROM lifelog_categories WHERE id=?", personalId);
        assertThat(jdbc.queryForObject("SELECT personal_category_id FROM collection_logs WHERE id=78101", Long.class))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT category FROM collection_logs WHERE id=78101", String.class))
                .isEqualTo("PROJECT");
    }

    private Flyway flyway(String target) {
        var config = Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration");
        if (target != null) config.target(MigrationVersion.fromVersion(target));
        return config.load();
    }
}
